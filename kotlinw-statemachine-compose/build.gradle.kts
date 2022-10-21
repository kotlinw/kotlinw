import org.jetbrains.compose.compose

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
    jvm { }
    js(IR) {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.kotlinw.kotlinwStatemachineCore)
                api(compose.runtime)
            }
        }
        val commonTest by getting {
        }
        val jvmMain by getting {
            dependencies {
            }
        }
        val jvmTest by getting {
            dependencies {
            }
        }
// HOMEAUT-123
//        val jsMain by getting {
//            dependencies {
//            }
//        }
//        val jsTest by getting {
//            dependencies {
//            }
//        }
    }
}
