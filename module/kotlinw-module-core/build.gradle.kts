import kotlinw.project.gradle.*

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    targetHierarchy.default()
    jvm { }
    js(IR) {
        browser()
    }
    if (isNativeTargetEnabled()) {
        mingwX64()
        linuxX64()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.kotlinw.kotlinwConfigurationCore)
                api(projects.kotlinw.kotlinwUtilCoroutineMp)
                api(projects.kotlinw.kotlinwEventbusLocal)
                api(projects.kotlinw.kotlinwLoggingApi)
                api(projects.kotlinw.kotlinwLoggingPlatform) // TODO implementation
                api(projects.kotlinw.kotlinwRemotingCore)
                api(projects.kotlinw.kotlinwRemotingClientKtor)

                api(libs.ktor.client.core)
            }
        }
        val jvmMain by getting {
            dependencies {
                api(libs.ktor.client.java)
            }
        }
        val jsMain by getting {
            dependencies {
                api(libs.ktor.client.js)
            }
        }
        if (isNativeTargetEnabled()) {
            val mingwX64Main by getting {
                dependencies {
                    api(libs.ktor.client.winhttp)
                }
            }
            val linuxX64Main by getting {
                dependencies {
                    api(libs.ktor.client.cio)
                }
            }
        }
    }
}
