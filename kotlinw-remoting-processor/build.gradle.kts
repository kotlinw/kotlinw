plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

kotlin {
    dependencies {
        implementation(projects.kotlinw.kotlinwRemotingApi)
        implementation(libs.ksp.api)
        implementation(libs.kotlinpoet.ksp)
        implementation(libs.kotlinx.collections.immutable)
        implementation(libs.kotlinx.datetime)
        implementation(libs.kotlinx.serialization.json)

        testImplementation(kotlin("test-junit5"))
        testImplementation(libs.logback.classic)
        testImplementation(libs.tschuchortdev.compiletesting.ksp)
    }
}
