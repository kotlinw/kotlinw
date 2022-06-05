plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm {  }
        js(IR) {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":kotlinw-utils"))
                api(project(":kotlinw-immutator-annotations"))
                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.json)
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
                api(kotlin("stdlib-jdk8"))
                api(kotlin("reflect"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("ch.qos.logback:logback-classic:1.2.5")
            }
        }
    }
}
