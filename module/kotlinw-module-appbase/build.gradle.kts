
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
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
                api(projects.kotlinw.module.kotlinwModuleCore)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
