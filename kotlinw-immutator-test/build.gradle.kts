import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
    id("com.google.devtools.ksp")
}

kotlin {
    applyDefaultHierarchyTemplate()
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
                implementation(projects.kotlinw.kotlinwImmutatorApi)

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
        val jsMain by getting {
            dependencies {
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
        val jvmMain by getting {
            dependencies {
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation(libs.logback.classic)
            }
        }
    }
}

compose {
//    kotlinCompilerPlugin = dependencies.compiler.forKotlin("1.8.20")
//    kotlinCompilerPluginArgs.add("suppressKotlinVersionCompatibilityCheck=2.0.0")
}

dependencies {
    add("kspCommonMainMetadata", projects.kotlinw.kotlinwImmutatorProcessor)
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
