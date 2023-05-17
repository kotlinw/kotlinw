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
    implementation(projects.kotlinw.kotlinwLoggingJvmSlf4j)
    implementation(projects.kotlinw.kotlinwLoggingSpi)
    implementation(libs.logback.classic)

    testImplementation(kotlin("test"))
}
