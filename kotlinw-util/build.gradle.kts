plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm { }
    js(IR) {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.kotlinw.kotlinwImmutatorAnnotations)
                api(projects.kotlinw.kotlinwUuid)

                api(libs.kotlinx.collections.immutable)
                // TODO legyen egy külön projekt, ahol ez Android specifikus actual: api("androidx.collection:collection:1.3.0-alpha02")
                api(libs.kotlinx.serialization.json)
                api(libs.kotlinx.datetime)
                api(libs.kotlinx.coroutines.core)

                api("com.michael-bull.kotlin-result:kotlin-result:1.1.16")
                api(libs.kotlin.logging)
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
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}
