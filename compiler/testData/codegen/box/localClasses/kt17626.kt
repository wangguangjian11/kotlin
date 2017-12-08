class Outer(val x: String) {
    open inner class Inner {
        fun foo() = x
    }

    fun test(): String {
        open class Local1 : Inner()

        class Local2 : Local1() {
            fun bar() = foo()
        }

        return Local2().bar()
    }
}

fun box() = Outer("OK").test()