
apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(project(":core:util.runtime"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:frontend.script"))
    compileOnly(project(":kotlin-reflect-api"))
    compile(ideaSdkCoreDeps(*(rootProject.extra["ideaCoreSdkJars"] as Array<String>)))
    compile("org.jetbrains.intellij.deps:trove4j:1.0.20160824")
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
