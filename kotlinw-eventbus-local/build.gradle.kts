plugins {
    `kotlinw-multiplatform-library`
    id("org.jetbrains.kotlinx.kover")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.kotlinw.kotlinwUtilStdlibMp)
                api(projects.kotlinw.kotlinwLoggingApi)
                api(libs.kotlinx.coroutines.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(projects.kotlinw.kotlinwLoggingPlatform)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

koverReport {
    filters {
        includes {
            classes("kotlinw.eventbus.local.*")
        }
    }
}
