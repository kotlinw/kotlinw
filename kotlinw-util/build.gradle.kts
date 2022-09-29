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
                api(project(":lib:kotlinw:kotlinw-immutator-annotations"))
                api(libs.kotlinx.serialization.json)
                api(libs.kotlinx.collections.immutable)
                api(libs.kotlin.logging)
                api(libs.kotlinx.datetime)
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
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(project.dependencies.platform(libs.kotlinjs.wrappers.bom))
                api(libs.kotlinjs.wrappers.js)
                implementation(npm("uuid", "8.3.2")) // TODO külön lib-be
                implementation(npm("@types/uuid", "8.3.1")) // TODO külön lib-be
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}
