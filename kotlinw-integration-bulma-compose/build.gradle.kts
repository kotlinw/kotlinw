plugins {
    `kotlinw-js-library`
    id("org.jetbrains.compose")
}

kotlin {
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.html.core)
                implementation(libs.jetbrains.compose.web.core)

                implementation(npm("bulma", "0.9.4"))
            }
        }
        val jsTest by getting
    }
}
