import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
}

kotlin {
    targetHierarchy.default()
    jvm { }
    linuxX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.kotlinw.kotlinwRemotingApi)
                implementation(projects.kotlinw.kotlinwRemotingCore)
                api(projects.kotlinw.kotlinwRemotingCoreKtor)
                api(libs.ktor.server.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(projects.kotlinw.kotlinwRemotingProcessorTest)
                implementation(projects.kotlinw.kotlinwRemotingClientKtor)
                implementation(libs.ktor.client.mock)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.server.test.host)
                implementation(libs.ktor.client.logging)
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.logback.classic)
                implementation(libs.mockk)
            }
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", projects.kotlinw.kotlinwRemotingProcessor)
    add("kspJvm", projects.kotlinw.kotlinwRemotingProcessor)
    add("kspJvmTest", projects.kotlinw.kotlinwRemotingProcessor)
}
