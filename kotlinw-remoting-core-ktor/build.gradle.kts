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
                api(projects.kotlinw.kotlinwRemotingApi)
                api(projects.kotlinw.kotlinwRemotingCore)
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
