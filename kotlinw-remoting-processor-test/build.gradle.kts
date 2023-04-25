import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
}

kotlin {
    jvm { }

//    js(IR) {
//        browser {}
//    }

//    val hostOs = System.getProperty("os.name")
//    val isMingwX64 = hostOs.startsWith("Windows")
//    val nativeTarget = when {
//        hostOs == "Mac OS X" -> macosX64("native")
//        hostOs == "Linux" -> linuxX64("native")
//        isMingwX64 -> mingwX64("native")
//        else -> throw GradleException("Host OS is not supported in Kotlin/Native: $hostOs")
//    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.kotlinw.kotlinwRemotingApi)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(projects.kotlinw.kotlinwRemotingClientKtor)
                implementation(libs.mockk)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        val jvmMain by getting {
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.logback.classic)
            }
        }
//        val jsMain by getting {
//        }
//        val jsTest by getting {
//        }
    }
}

dependencies {
    add("kspCommonMainMetadata", projects.kotlinw.kotlinwRemotingProcessor)
//    add("kspJs", projects.kotlinw.kotlinwRemotingProcessor)
//    add("kspJsTest", projects.kotlinw.kotlinwRemotingProcessor)
//    add("kspJvm", projects.kotlinw.kotlinwRemotingProcessor)
//    add("kspJvmTest", projects.kotlinw.kotlinwRemotingProcessor)
}

tasks.withType<KotlinCompile<*>>().all {
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}

kotlin.sourceSets.commonMain {
    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
}
