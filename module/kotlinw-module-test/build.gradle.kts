plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
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
                // TODO this does not propagate as an API dependency: api(libs.kotlin.test)
                api(projects.kotlinw.module.kotlinwModuleCore)
                api(projects.kotlinw.module.kotlinwModuleServerbase)
                api(libs.kotlinx.coroutines.test)
                api(libs.ktor.server.test.host)
            }
        }
        val jvmMain by getting {
            dependencies {
                api(libs.mockk)
            }
        }
    }
}
