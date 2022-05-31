plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

kotlin {
    dependencies {
        implementation(project(":kotlinw-immutator-api"))

        implementation("com.google.devtools.ksp:symbol-processing-api:1.6.10-1.0.4")
        implementation("com.squareup:kotlinpoet-ksp:1.11.0")

        implementation(kotlin("stdlib-jdk8"))
        testImplementation(kotlin("test-junit5"))
        testImplementation("ch.qos.logback:logback-classic:1.2.5")
        testImplementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:1.4.8")
    }
}
