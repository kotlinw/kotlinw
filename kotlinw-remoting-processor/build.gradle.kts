plugins {
    kotlin("jvm")
}

kotlin {
    target {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 19)
        }
        compilations.all {
            kotlinOptions {
                jvmTarget = "19"
            }
        }
    }
}

dependencies {
    implementation(projects.kotlinw.kotlinwRemotingApi)
    implementation(projects.kotlinw.kotlinwKspUtil)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(projects.kotlinw.kotlinwKspTestutil)
    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.logback.classic)
}
