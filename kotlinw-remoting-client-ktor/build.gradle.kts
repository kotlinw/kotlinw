plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm { }
    js(IR) {
        browser()
    }

    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native: $hostOs")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.kotlinw.kotlinwRemotingApi)
                implementation(projects.kotlinw.kotlinwRemotingCore)
                api(projects.kotlinw.kotlinwRemotingCoreKtor)
                api(libs.ktor.client.core)
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
    }
}
