import org.jetbrains.compose.compose

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
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
                implementation(projects.kotlinw.kotlinwImmutatorApi)
                implementation(compose.runtime)
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
                implementation(libs.logback.classic)
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
//    add("kspMetadata", projects.lib.kotlinw:kotlinw-immutator-processor)
    add("kspJvm", projects.kotlinw.kotlinwImmutatorProcessor)
    add("kspJvmTest", projects.kotlinw.kotlinwImmutatorProcessor)
}

compose {
    kotlinCompilerPlugin.set(dependencies.compiler.forKotlin("1.8.20"))
    kotlinCompilerPluginArgs.add("suppressKotlinVersionCompatibilityCheck=1.8.21")
}
