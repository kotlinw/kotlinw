plugins {
    `kotlinw-multiplatform`
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
