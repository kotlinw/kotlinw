plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

kotlin {
    targetHierarchy.default()
    js(IR) {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.kotlinw.kotlinwMarkdownCore)
                api(compose.runtime)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        val jsMain by getting {
            dependencies {
                api(libs.jetbrains.compose.html.core)
                api(compose.html.core)
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

//compose {
//    kotlinCompilerPlugin.set(dependencies.compiler.forKotlin("1.9.21"))
//    kotlinCompilerPluginArgs.add("suppressKotlinVersionCompatibilityCheck=1.9.22")
//}
