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
        commonMain {
            dependencies {
                api(projects.kotlinw.kotlinwOauth2Core)

                api(libs.ktor.server.auth.core)
            }
        }
    }
}
