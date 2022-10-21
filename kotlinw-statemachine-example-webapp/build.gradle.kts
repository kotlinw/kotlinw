import org.jetbrains.compose.compose

plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp")
    id("org.jetbrains.compose")
}

kotlin {
    jvm()
    js(IR) {
        browser {}
        binaries.executable()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.kotlinw.kotlinwImmutatorApi)
                implementation(projects.kotlinw.kotlinwStatemachineCompose)
                implementation(compose.runtime)
            }
        }
        val jsMain by getting {
            kotlin.srcDir("build/generated/ksp/js/jsMain/kotlin")
            dependencies {
                implementation(compose.web.core)
            }
        }
        val jvmMain by getting {
            dependencies {
            }
        }
    }
}

dependencies {
    add("kspJs", projects.kotlinw.kotlinwImmutatorProcessor)
}
