/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.synthetic

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.descriptors.synthetic.SyntheticMemberDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.ResolutionScope
import org.jetbrains.kotlin.resolve.scopes.SyntheticResolutionScope
import org.jetbrains.kotlin.resolve.scopes.SyntheticScopeProvider
import org.jetbrains.kotlin.resolve.scopes.SyntheticScopesMetadata
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.isInterface
import kotlin.properties.Delegates

interface CompatSyntheticFunctionDescriptor: FunctionDescriptor, SyntheticMemberDescriptor<FunctionDescriptor>

private class CompatSyntheticMemberScope(
        storageManager: StorageManager,
        private val type: KotlinType,
        override val wrappedScope: ResolutionScope
) : SyntheticResolutionScope() {
    private val ownerClass = type.constructor.declarationDescriptor as ClassDescriptor
    private val annotationFqName = FqName("kotlin.annotations.jvm.internal.Compat")
    private val functions = storageManager.createMemoizedFunction<Name, Collection<FunctionDescriptor>> {
        shadowOriginalFunctions(super.getContributedFunctions(it, NoLookupLocation.FROM_SYNTHETIC_SCOPE), doGetFunctions(it))
    }
    private val compats = storageManager.createLazyValue {
        doGetCompats()
    }

    private fun doGetCompats(): Collection<ClassDescriptor> {
        return (type.constructor.supertypes + type)
                .filterNot { it.isInterface() }
                .mapNotNull { (it.constructor.declarationDescriptor as? ClassDescriptor)?.findCompat() }
    }

    private fun ClassDescriptor.findCompat(): ClassDescriptor? {
        val annotation = annotations.firstOrNull { it.fqName == annotationFqName } ?: return null
        val annotationValue = annotation.argumentValue("value") ?: error("Compat annotation must have value")
        val valueString = annotationValue as? String ?: error("$annotationValue must be string")

        var packageName = ""
        var className = valueString
        if (valueString.contains('.')) {
            packageName = valueString.substring(0, valueString.lastIndexOf('.'))
            className = valueString.substring(valueString.lastIndexOf('.') + 1)
        }

        return module.getPackage(FqName(packageName))
                       .memberScope
                       .getContributedClassifier(
                               Name.identifier(className),
                               NoLookupLocation.FROM_SYNTHETIC_SCOPE
                       ) as? ClassDescriptor ?: error("Compat must be a class")
    }

    private fun doGetFunctions(name: Name): Collection<FunctionDescriptor> {
        return compats().flatMap { compat ->
            val compatFunctions = compat.staticScope.getContributedFunctions(name, NoLookupLocation.FROM_SYNTHETIC_SCOPE)
            val visibleCompatFunctions = compatFunctions.filter { it.visibility == Visibilities.PUBLIC }
            val visibleCompatFunctionsWhichTakeOriginAsFistParam = visibleCompatFunctions.filter {
                it.valueParameters.size > 0 && KotlinTypeChecker.DEFAULT.isSubtypeOf(type, it.valueParameters.first().type)
            }
            visibleCompatFunctionsWhichTakeOriginAsFistParam.map { MyFunctionDescriptor.create(ownerClass, it) }
        }
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
        return functions(name)
    }

    private fun shadowOriginalFunctions(
            originals: Collection<FunctionDescriptor>,
            synthetics: Collection<FunctionDescriptor>
    ): Collection<FunctionDescriptor> {
        return synthetics + originals.filterNot { original ->
            synthetics.any { it.shadows(original) }
        }
    }

    private fun FunctionDescriptor.shadows(other: FunctionDescriptor): Boolean {
        if (name != other.name) return false
        if (containingDeclaration != other.containingDeclaration) return false
        if (valueParameters.size != other.valueParameters.size) return false
        if (returnType == null) {
            if (other.returnType != null) return false
        } else {
            if (other.returnType == null) return false
            if (!KotlinTypeChecker.DEFAULT.equalTypes(returnType!!, other.returnType!!)) return false
        }
        for (i in 0 until valueParameters.size) {
            if (!KotlinTypeChecker.DEFAULT.equalTypes(valueParameters[i].type, other.valueParameters[i].type)) return false
        }
        return true
    }

    // Looks like origin function, acts like compat function
    private class MyFunctionDescriptor(
            containingDeclaration: DeclarationDescriptor,
            original: SimpleFunctionDescriptor?,
            annotations: Annotations,
            name: Name,
            kind: CallableMemberDescriptor.Kind,
            source: SourceElement
    ) :
            SimpleFunctionDescriptorImpl(containingDeclaration, original, annotations, name, kind, source),
            CompatSyntheticFunctionDescriptor
    {
        override var baseDescriptorForSynthetic: FunctionDescriptor by Delegates.notNull()
            private set

        companion object {
            fun create(ownerClass: ClassDescriptor, compat: FunctionDescriptor): MyFunctionDescriptor {
                val result = MyFunctionDescriptor(
                        ownerClass,
                        null,
                        compat.annotations,
                        compat.name,
                        CallableMemberDescriptor.Kind.SYNTHESIZED,
                        compat.original.source
                )
                result.baseDescriptorForSynthetic = compat
                val valueParams = compat.valueParameters.drop(1).map {
                    ValueParameterDescriptorImpl(
                            it.containingDeclaration,
                            it,
                            it.index - 1,
                            it.annotations,
                            it.name,
                            it.type,
                            it.declaresDefaultValue(),
                            it.isCrossinline,
                            it.isNoinline,
                            it.varargElementType,
                            it.source
                    )
                }
                result.initialize(
                        null,
                        ownerClass.thisAsReceiverParameter,
                        compat.typeParameters,
                        valueParams,
                        compat.returnType,
                        compat.modality,
                        compat.visibility
                )
                return result
            }
        }
    }
}

class CompatSyntheticMembersProvider(storageManager: StorageManager) : SyntheticScopeProvider {
    private val makeSynthetic = storageManager.createMemoizedFunction<Pair<KotlinType, ResolutionScope>, ResolutionScope> { (type, scope) ->
        CompatSyntheticMemberScope(storageManager, type, scope)
    }

    override fun provideSyntheticScope(scope: ResolutionScope, metadata: SyntheticScopesMetadata): ResolutionScope {
        val type = metadata.type
        // The property can be generated from getters, thus, we should provide a scope for them
        val needCompat = metadata.needMemberFunctions || metadata.needExtensionProperties
        if (!needCompat || type == null || type.constructor.declarationDescriptor !is ClassDescriptor) return scope
        return makeSynthetic(Pair(type, scope))
    }
}