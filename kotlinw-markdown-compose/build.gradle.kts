plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

kotlin {
    targetHierarchy.default()
    // TODO jvm { }
    js(IR) {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.kotlinw.kotlinwMarkdownCore)
                api(compose.runtime)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        val jsMain by getting {
            dependencies {
                api(libs.jetbrains.compose.web.core)
                api(compose.html.core)
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
// TODO        val jvmMain by getting {
//            dependencies {
//            }
//        }
//        val jvmTest by getting {
//            dependencies {
//                implementation(kotlin("test-junit5"))
//                implementation(libs.logback.classic)
//            }
//        }
//        val desktopMain by creating {
//            dependsOn(jvmMain)
//            dependencies {
//                api(compose.desktop.currentOs)
//                api(compose.foundation)
//                api(compose.material)
//            }
//        }
//        val desktopTest by creating {
//            dependsOn(jvmTest)
//        }
    }
}
