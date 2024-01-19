plugins {
    kotlin("multiplatform") version "1.9.22"
}

group = "org.example"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    sourceSets.all {
        languageSettings {
            languageVersion = "1.9"
            apiVersion = "1.9"
        }
    }
    jvm {
        jvmToolchain(19)
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    js(IR) {
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinw.util.stdlib)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting
        val jvmTest by getting
        val jsMain by getting
        val jsTest by getting
    }
}
