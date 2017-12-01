
apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(ideaSdkCoreDeps("intellij-core"))
    compile("org.jetbrains.intellij.deps:trove4j:1.0.20160824")
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

