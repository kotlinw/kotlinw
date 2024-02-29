
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
                api(projects.kotlinw.module.kotlinwModuleApi)
                api(projects.kotlinw.kotlinwConfigurationCore)
                api(projects.kotlinw.kotlinwEventbusInprocess)
                api(projects.kotlinw.kotlinwLoggingApi)
                api(projects.kotlinw.kotlinwLoggingPlatform) // TODO implementation
                api(projects.kotlinw.kotlinwSerializationCore)
                api(projects.kotlinw.kotlinwUtilCoroutineMp)
                api(projects.kotlinw.kotlinwUtilStdlibMp)
                api(libs.ktor.client.content.negotiation)
                api(libs.ktor.client.logging)
                implementation(libs.ktor.serialization.kotlinx.json)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(projects.kotlinw.kotlinwLoggingPlatform)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(projects.kotlinw.kotlinwLoggingJvmLogback)
                implementation(libs.ktor.client.java)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.mockk)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js)
            }
        }
        if (isNativeTargetEnabled()) {
            val mingwX64Main by getting {
                dependencies {
                    implementation(libs.ktor.client.winhttp)
                }
            }
            val linuxX64Main by getting {
                dependencies {
                    implementation(libs.ktor.client.cio)
                }
            }
        }
    }
}
