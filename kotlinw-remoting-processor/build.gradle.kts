plugins {
    kotlin("jvm")
}

kotlin {
    target {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
        }
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
}

dependencies {
    implementation(projects.kotlinw.kotlinwRemotingApi)
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet.ksp)
    implementation(libs.kotlinpoet.metadata)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.logback.classic)
    testImplementation(libs.tschuchortdev.compiletesting.ksp)
}