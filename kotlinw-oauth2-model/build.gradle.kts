plugins {
    `kotlinw-multiplatform-library`
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlinx.serialization.core)
                api(projects.kotlinw.kotlinwUtilStdlibMp)
            }
        }
    }
}
