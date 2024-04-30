plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
}

kotlin {
    applyDefaultHierarchyTemplate()
    jvm { }
    if (isNativeTargetEnabled()) {
        linuxX64()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.kotlinw.module.kotlinwModuleAppbase)
                api(projects.kotlinw.module.kotlinwModuleKtorServer)
                api(projects.kotlinw.module.kotlinwModuleRemotingServer)
                api(projects.kotlinw.kotlinwUtilCoroutineMp)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                // implementation(libs.ktor.server.test.host)
                // implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.cio)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.ktor.server.netty)
            }
        }
    }
}

dependencies {
//    add("kspCommonMainMetadata", projects.kotlinw.kotlinwDiProcessor)
//    add("kspJvm", projects.kotlinw.kotlinwDiProcessor)
    add("kspJvmTest", projects.kotlinw.kotlinwDiProcessor)
}

//tasks.withType<KotlinCompile<*>>().configureEach {
//    if (name != "kspCommonMainKotlinMetadata") {
//        dependsOn("kspCommonMainKotlinMetadata")
//    }
//}

//kotlin.sourceSets.commonMain {
//    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
//}
