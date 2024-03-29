plugins {
    `kotlinw-multiplatform-library`
}

kotlin {
    compilerOptions {
        freeCompilerArgs.set(freeCompilerArgs.get() + "-Xcontext-receivers") // TODO why is this necessary? theoretically this is in the root build file as well...
    }

    sourceSets {
        commonMain {
            dependencies {
                api(projects.kotlinw.kotlinwJwtCore)
                api(projects.kotlinw.kotlinwUtilCoroutineMp)
                api(projects.kotlinw.kotlinwUtilKtorClient)
                api(projects.kotlinw.kotlinwUtilSerializationJson)
                api(projects.kotlinw.kotlinwUtilStdlibMp)
                api(projects.kotlinw.kotlinwLoggingPlatform)

                api(libs.kotlinx.serialization.json)
                api(libs.kotlinx.datetime)
                api(libs.ktor.client.core)

                api(libs.ktor.client.serialization)
                api(libs.ktor.serialization.kotlinx.json)
                api(libs.ktor.client.content.negotiation)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.ktor.client.logging)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.logback.classic)
            }
        }
        jsTest {
            dependencies {
                implementation(libs.ktor.client.js)
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.ktor.client.java)
            }
        }
    }
}
