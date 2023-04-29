import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
}

kotlin {
    targetHierarchy.default()
    jvm { }
    js(IR) {
        browser()
    }
    mingwX64()
    linuxX64()

    sourceSets {
        val commonMain by getting {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
            dependencies {
                api(projects.kotlinw.kotlinwRemotingApi)
                api(projects.kotlinw.kotlinwUtilStdlibMp)
                api(libs.kotlinx.datetime)
                api(libs.okio.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                api(libs.kotlinx.serialization.json)
                api(libs.kotlinx.coroutines.test)
            }
        }
        val jvmMain by getting {
            kotlin.srcDir("build/generated/ksp/jvm/jvmMain/kotlin")
            dependencies {
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", projects.kotlinw.kotlinwRemotingProcessor)
//    add("kspJs", projects.kotlinw.kotlinwRemotingProcessor)
//    add("kspJsTest", projects.kotlinw.kotlinwRemotingProcessor)
    add("kspJvm", projects.kotlinw.kotlinwRemotingProcessor)
    add("kspJvmTest", projects.kotlinw.kotlinwRemotingProcessor)
}

tasks.withType<KotlinCompile<*>>().all {
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}
