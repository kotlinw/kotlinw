plugins {
    `kotlinw-multiplatform-library`
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.kotlinw.kotlinwJwtCore)
                api(projects.kotlinw.kotlinwOauth2ClientCore)

                api(libs.ktor.client.core)
            }
        }
    }
}
