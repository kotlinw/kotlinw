import org.jetbrains.compose.compose

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
    jvm { }
// HOMEAUT-123
//    js(IR) {
//        browser()
//    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":kotlinw-util"))
                api(compose.runtime)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                api(compose.foundation)
                api(compose.material)
                api(compose.desktop.currentOs)
                api(compose.preview)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("ch.qos.logback:logback-classic:1.2.5")
            }
        }
// HOMEAUT-123
//        val jsMain by getting {
//            dependencies {
//                implementation(compose.web.core)
//            }
//        }
//        val jsTest by getting {
//            dependencies {
//                implementation(kotlin("test-js"))
//            }
//        }
    }
}
