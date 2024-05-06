
plugins {
    `kotlinw-multiplatform-library`
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.kotlinw.kotlinwUtilCoroutineMp)
                implementation(projects.kotlinw.kotlinwUtilStdlibMp)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
    }
}
