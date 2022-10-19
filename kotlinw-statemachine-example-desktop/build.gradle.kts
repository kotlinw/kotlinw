import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    id("org.jetbrains.compose")
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
    implementation(project(":kotlinw:kotlinw-compose"))
    implementation(project(":kotlinw:kotlinw-statemachine-compose"))
    implementation(compose.desktop.currentOs)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material)
    implementation(compose.preview)
}
