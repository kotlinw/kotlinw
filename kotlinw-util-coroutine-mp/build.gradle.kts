
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.set(freeCompilerArgs.get() + "-Xcontext-receivers") // TODO why is this necessary? theoretically this is in the root build file as well...
    }

    applyDefaultHierarchyTemplate()
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
                api(projects.kotlinw.kotlinwUtilStdlibMp)

                api(libs.kotlinx.coroutines.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.turbine)
            }
        }
        val jvmMain by getting {
            dependencies {
                api(kotlin("reflect"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jsMain by getting {
            dependencies {
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
        if (isNativeTargetEnabled()) {
            val nativeMain by getting
            val nativeTest by getting
        }
    }
}
