public class JavaUser {
    // Dangerous
    @Ann(KotlinPropertiesKt.importantString)
    public void foo() {}

    // Also dangerous
    @Ann(OtherPropertiesKt.notSoImportantString)
    public void bar() {}

    // Safe
    @Ann(KotlinPropertiesKt.constantString)
    public void baz() {}
}