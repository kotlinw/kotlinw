plugins {
    `kotlinw-multiplatform`
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.kotlinw.kotlinwOauth2Model)
                api(projects.kotlinw.kotlinwUtilCoroutineMp)

                implementation(libs.ktor.client.serialization)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.content.negotiation)
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
