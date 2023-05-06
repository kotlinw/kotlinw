import kotlinw.project.gradle.*

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    targetHierarchy.default()
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
                api(projects.kotlinw.kotlinwImmutatorAnnotations)
                api(projects.kotlinw.kotlinwUtilCoroutineMp)
                api(projects.kotlinw.kotlinwUtilDatetimeMp)
                api(projects.kotlinw.kotlinwUtilStdlibMp)
            }
        }
    }
}
