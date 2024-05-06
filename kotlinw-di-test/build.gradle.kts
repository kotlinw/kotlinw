
plugins {
    `kotlinw-multiplatform-library`
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.kotlinw.kotlinwDiApi)
                api(libs.kotlin.test)
            }
        }
    }
}
