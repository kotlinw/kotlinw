plugins {
    `kotlinw-multiplatform-library`
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.kotlinw.kotlinwIo)
                api(projects.kotlinw.kotlinwUtilStdlibMp)
                api(projects.shared.kotlinwI18nCoreMp)
                api(projects.kotlinw.kotlinwLoggingPlatform)

                api(libs.kotlinx.serialization.core)
                api(libs.kotlinx.io.core)

                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}
