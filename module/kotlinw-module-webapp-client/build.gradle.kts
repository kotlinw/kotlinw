plugins {
    `kotlinw-js-library`
    id("org.jetbrains.compose")
}

kotlin {
    sourceSets {
        val jsMain by getting {
            dependencies {
                api(projects.kotlinw.module.kotlinwModuleAppbase)
                api(libs.kotlinjs.wrappers.browser)
                api(compose.html.core)
                api(compose.html.svg)
                api(compose.runtime)
                api(libs.kobweb.compose.core)
                api(libs.kobweb.compose.html.ext)
            }
        }
    }
}
