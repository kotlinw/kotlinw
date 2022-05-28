plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm {  }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.kotlinx.collections.immutable)
                api(libs.kotlinx.datetime)
                api(libs.kotlinx.serialization.json)
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
