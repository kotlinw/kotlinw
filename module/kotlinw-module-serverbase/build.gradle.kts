plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    targetHierarchy.default()
    jvm { }
    if (isNativeTargetEnabled()) {
        linuxX64()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.kotlinw.module.kotlinwModuleAppbase)
                api(projects.kotlinw.module.kotlinwModuleKtorServer)
                api(projects.kotlinw.kotlinwUtilCoroutineMp)
                api(projects.kotlinw.kotlinwRemotingApi)
                api(projects.kotlinw.kotlinwRemotingServerKtor)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                // implementation(libs.ktor.server.test.host)
                // implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.cio)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.ktor.server.netty)
            }
        }
    }
}
