import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
}

kotlin {
    targetHierarchy.default()
    jvm { }
    if (isNativeTargetEnabled()) {
        linuxX64()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.kotlinw.module.kotlinwModuleKtorServer)
                api(projects.kotlinw.kotlinwUtilCoroutineMp)
                api(projects.kotlinw.kotlinwRemotingApi)
                api(projects.kotlinw.kotlinwRemotingServerKtor)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
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
