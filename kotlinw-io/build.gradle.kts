plugins {
    `kotlinw-multiplatform-library`
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlinx.io.bytestring)
                api(libs.kotlinx.io.core)
                api(libs.kotlinx.serialization.core)
                implementation(libs.ktor.http)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        jvmMain {
            dependencies {
                implementation(libs.classgraph)
            }
        }
    }
}
