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
    if (isNativeTargetEnabled()) {
        linuxX64()
    }

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
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        val jvmMain by getting {
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.logback.classic)
                implementation(libs.mockk)
            }
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", projects.kotlinw.kotlinwRemotingProcessor)
//    add("kspJs", projects.kotlinw.kotlinwRemotingProcessor)
//    add("kspJsTest", projects.kotlinw.kotlinwRemotingProcessor)
//    add("kspJvm", projects.kotlinw.kotlinwRemotingProcessor)
//    add("kspJvmTest", projects.kotlinw.kotlinwRemotingProcessor)
}

tasks.withType<KotlinCompile<*>>().configureEach {
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}

kotlin.sourceSets.commonMain {
    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
}
