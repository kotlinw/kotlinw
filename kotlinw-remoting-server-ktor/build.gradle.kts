import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
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
                implementation(projects.kotlinw.kotlinwRemotingCore)
                api(projects.kotlinw.kotlinwRemotingCoreKtor)
                api(libs.ktor.server.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(projects.kotlinw.kotlinwRemotingProcessorTest)
                implementation(projects.kotlinw.kotlinwRemotingClientKtor)
                implementation(libs.mockk)
                implementation(libs.ktor.client.mock)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.server.test.host)
                implementation(libs.ktor.client.logging)
                implementation(libs.logback.classic)
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
            }
        }
        val jvmTest by getting {
            dependencies {
            }
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", projects.kotlinw.kotlinwRemotingProcessor)
    add("kspJvm", projects.kotlinw.kotlinwRemotingProcessor)
    add("kspJvmTest", projects.kotlinw.kotlinwRemotingProcessor)
}
