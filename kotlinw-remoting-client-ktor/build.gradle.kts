
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    applyDefaultHierarchyTemplate()
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
                api(projects.kotlinw.kotlinwRemotingApi)
                implementation(projects.kotlinw.kotlinwRemotingCore)
                api(projects.kotlinw.kotlinwRemotingCoreKtor)
                implementation(projects.kotlinw.kotlinwUtilCoroutineMp)
                api(libs.ktor.client.core)
                api(libs.ktor.client.websockets)
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
