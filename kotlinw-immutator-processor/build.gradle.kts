plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

kotlin {
    dependencies {
        implementation(project(":kotlinw-immutator-api"))
        implementation("com.google.devtools.ksp:symbol-processing-api:1.6.21-1.0.5")
        implementation("com.squareup:kotlinpoet-ksp:1.11.0")
        implementation(kotlin("stdlib-jdk8"))
        implementation(libs.kotlinx.collections.immutable)
        implementation(libs.kotlinx.datetime)
        implementation(libs.kotlinx.serialization.json)

        testImplementation(kotlin("test-junit5"))
        testImplementation("ch.qos.logback:logback-classic:1.2.5")
        testImplementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:1.4.8")
    }
}
