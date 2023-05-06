
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
                api(projects.kotlinw.kotlinwLoggingSpi)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jsMain by getting {
            dependencies {
                api(projects.kotlinw.kotlinwLoggingJsConsole)
            }
        }
        val jvmMain by getting {
            dependencies {
                api(projects.kotlinw.kotlinwLoggingJvmSlf4j)
                implementation(libs.logback.classic)
            }
        }
        if (isNativeTargetEnabled()) {
            val nativeMain by getting {
                dependencies {
                    api(projects.kotlinw.kotlinwLoggingStdout)
                }
            }
        }
    }
}
