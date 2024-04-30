plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    applyDefaultHierarchyTemplate()
    jvm { }
    if (isNativeTargetEnabled()) {
        linuxX64()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.kotlinw.module.kotlinwModuleCore)
                api(libs.ktor.server.auth.core)
                api(libs.ktor.server.caching.headers)
                api(libs.ktor.server.sessions)
                api(libs.ktor.server.core)
                api(libs.ktor.server.host.common)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                // TODO implementation(libs.ktor.server.test.host)
                implementation(libs.ktor.server.cio)
            }
        }
    }
}
