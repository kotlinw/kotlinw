plugins {
    `kotlinw-multiplatform-library`
    // TODO kover: id("org.jetbrains.kotlinx.kover")
}

apply(plugin = "kotlinx-knit")

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

// TODO kover: koverReport {
//    filters {
//        includes {
//            classes("kotlinw.eventbus.local.*")
//        }
//    }
//}
