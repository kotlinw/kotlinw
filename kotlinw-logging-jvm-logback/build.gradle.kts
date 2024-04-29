plugins {
    kotlin("jvm")
}

kotlin {
    target {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 11)
        }
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
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
