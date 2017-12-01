
apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    val compile by configurations
    compile(project(":compiler:cli"))
    compile(project(":compiler:daemon-common"))
    compile(project(":compiler:incremental-compilation-impl"))
    compile(project(":kotlin-build-common"))
    compile(ideaSdkCoreDeps(*(rootProject.extra["ideaCoreSdkJars"] as Array<String>)))
    compile("org.jetbrains.intellij.deps:trove4j:1.0.20160824")
    compile(commonDep("org.fusesource.jansi", "jansi"))
    compile(commonDep("org.jline", "jline"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
