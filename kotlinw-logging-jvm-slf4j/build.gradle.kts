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
    implementation(projects.kotlinw.kotlinwLoggingSpi)
    implementation(libs.slf4j.api)
    implementation(libs.kotlinx.coroutines.slf4j)

    testImplementation(kotlin("test"))

    constraints {
        implementation(libs.slf4j.api)
    }
}
