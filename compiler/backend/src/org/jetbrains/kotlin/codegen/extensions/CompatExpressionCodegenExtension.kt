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

package org.jetbrains.kotlin.codegen.extensions

import org.jetbrains.kotlin.codegen.CallBasedArgumentGenerator
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.asmType
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.synthetic.CompatSyntheticFunctionDescriptor
import org.jetbrains.kotlin.synthetic.SamAdapterExtensionFunctionDescriptor
import org.jetbrains.org.objectweb.asm.Type

object CompatExpressionCodegenExtension : ExpressionCodegenExtension {
    override fun applyFunction(receiver: StackValue, resolvedCall: ResolvedCall<*>, c: ExpressionCodegenExtension.Context): StackValue? {
        var candidateDescriptor = resolvedCall.candidateDescriptor
        if (candidateDescriptor is SamAdapterExtensionFunctionDescriptor) {
            candidateDescriptor = candidateDescriptor.baseDescriptorForSynthetic
        }
        val compat = candidateDescriptor as? CompatSyntheticFunctionDescriptor ?: return null
        val receiverDescriptor = resolvedCall.dispatchReceiver!!.type.constructor.declarationDescriptor!!
        val baseDescriptor = compat.baseDescriptorForSynthetic
        val actualReceiver = StackValue.receiver(resolvedCall, receiver, c.codegen, null)
        val callable = c.typeMapper.mapToCallableMethod(compat, false)
        val baseCallable = c.typeMapper.mapToCallableMethod(baseDescriptor, false)

        actualReceiver.put(c.typeMapper.mapType(receiverDescriptor), c.v)
        val argGen = CallBasedArgumentGenerator(
                c.codegen,
                c.codegen.defaultCallGenerator,
                compat.valueParameters,
                callable.valueParameterTypes
        )
        argGen.generate(
                resolvedCall.valueArgumentsByIndex ?: emptyList(),
                resolvedCall.valueArguments.values.toList(),
                null
        )
        return StackValue.functionCall(baseDescriptor.returnType?.asmType(c.typeMapper) ?: Type.VOID_TYPE) { v ->
            v.invokestatic(
                    baseCallable.owner.internalName,
                    baseCallable.getAsmMethod().name,
                    baseCallable.getAsmMethod().descriptor,
                    false
            )
        }
    }
}