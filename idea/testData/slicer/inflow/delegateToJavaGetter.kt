// FLOW: IN
// RUNTIME_WITH_REFLECT

import kotlin.reflect.KProperty

object E {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = 1
}

val foo: Int by D.INSTANCE

fun test() {
    val <caret>x = foo
}