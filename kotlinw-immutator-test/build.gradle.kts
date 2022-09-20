import org.jetbrains.compose.compose

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
}

kotlin {
    jvm { }
// HOMEAUT-123
//    js(IR) {
//        browser {}
//    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":lib:kotlinw:kotlinw-immutator-api"))
                implementation(project(":lib:kotlinw:kotlinw-immutator-test2"))

                implementation(compose.runtime)
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
            kotlin.srcDir("build/generated/ksp/jvm/jvmMain/kotlin")
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation(kotlin("reflect"))
            }
        }
        val jvmTest by getting {
            kotlin.srcDir("build/generated/ksp/jvm/jvmTest/kotlin")
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("ch.qos.logback:logback-classic:1.2.5")
            }
        }
// HOMEAUT-123
//        val jsMain by getting {
//        }
//        val jsTest by getting {
//        }
    }
}

dependencies {
//    add("kspMetadata", project(":lib:kotlinw:kotlinw-immutator-processor"))
    add("kspJvm", project(":lib:kotlinw:kotlinw-immutator-processor"))
    add("kspJvmTest", project(":lib:kotlinw:kotlinw-immutator-processor"))
}
