import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp") version "1.6.10-1.0.4"
}

kotlin {
    jvm { }
//    js(IR) {
//        browser {
//            testTask {
//                useKarma {
//                    useChromeHeadless()
//                }
//            }
//        }
//    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":kotlinw-immutator-api"))

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
            kotlin.srcDir("build/generated/ksp/jvm/jvmTest/kotlin")
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("ch.qos.logback:logback-classic:1.2.5")
                implementation(project(":kotlinw-immutator-processor"))
            }
        }
//        val jsMain by getting {
//        }
//        val jsTest by getting {
//        }
    }
}

dependencies {
    add("kspMetadata", project(":kotlinw-immutator-processor"))
    add("kspJvm", project(":kotlinw-immutator-processor"))
    add("kspJvmTest", project(":kotlinw-immutator-processor"))
    // ksp("")
}
