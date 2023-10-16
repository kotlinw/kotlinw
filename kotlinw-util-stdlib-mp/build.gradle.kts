
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
                api(projects.kotlinw.kotlinwIo)
                api(libs.arrow.core)
                api(libs.kotlinresult.core)
                api(libs.kotlinx.collections.immutable)
                api(libs.kotlinx.serialization.core)
                api(libs.kotlinx.atomicfu)
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
                implementation(project.dependencies.platform(libs.kotlinjs.wrappers.bom))
                api(libs.kotlinjs.wrappers.js)
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
        if (isNativeTargetEnabled()) {
            val nativeMain by getting {
                dependencies {
                    implementation(libs.kotlinx.atomicfu)
                }
            }
            val nativeTest by getting {
                dependencies {
                }
            }
        }
    }
}
