plugins {
    `kotlinw-multiplatform-library`
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.kotlinw.kotlinwUtilStdlibMp)
                api(libs.kotlinx.io.bytestring)
                api(libs.kotlinx.io.core)
                api(libs.kotlinx.serialization.core)
                implementation(libs.ktor.http)
            }
        }
    }
}
