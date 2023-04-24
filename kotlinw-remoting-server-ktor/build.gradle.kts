plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm { }

    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    if (!isMingwX64) {
        // Windows is not supported as a Kotlin/Native server environment: https://ktor.io/docs/native-server.html

        val nativeTarget = when {
            hostOs == "Mac OS X" -> macosX64("native")
            hostOs == "Linux" -> linuxX64("native")
            else -> throw GradleException("Host OS is not supported in Kotlin/Native: $hostOs")
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.kotlinw.kotlinwRemotingApi)
                api(projects.kotlinw.kotlinwRemotingCore)
                api(libs.ktor.server.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.ktor.client.mock)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.kotlinx.serialization.json)
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.ktor.server.test.host)
            }
        }
    }
}
