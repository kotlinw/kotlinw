plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

kotlin {
    dependencies {
        implementation(projects.kotlinw.kotlinwImmutatorApi)
        implementation("com.google.devtools.ksp:symbol-processing-api:1.8.0-1.0.9")
        implementation("com.squareup:kotlinpoet-ksp:1.11.0")
        implementation(kotlin("stdlib-jdk8"))
        implementation(libs.kotlinx.collections.immutable)
        implementation(libs.kotlinx.datetime)
        implementation(libs.kotlinx.serialization.json)

        testImplementation(kotlin("test-junit5"))
        testImplementation(libs.logback.classic)
        testImplementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:1.4.8")
    }
}
