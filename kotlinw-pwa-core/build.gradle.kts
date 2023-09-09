plugins {
    `kotlinw-multiplatform`
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlinx.serialization.core)
                api(projects.kotlinw.kotlinwUtilStdlibMp)
                api(projects.shared.kotlinwI18nCoreMp)
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}
