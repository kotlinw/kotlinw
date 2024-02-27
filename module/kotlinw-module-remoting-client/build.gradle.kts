
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
                api(projects.kotlinw.module.kotlinwModuleCore)
                api(projects.kotlinw.module.kotlinwModuleRemotingShared)
                api(projects.kotlinw.kotlinwOauth2Core)
                api(projects.kotlinw.kotlinwRemotingClientKtor)
                api(projects.kotlinw.kotlinwOauth2KtorClient)
                api(libs.ktor.client.content.negotiation)
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
        val jvmTest by getting {
            dependencies {
                implementation(libs.mockk)
            }
        }
    }
}
