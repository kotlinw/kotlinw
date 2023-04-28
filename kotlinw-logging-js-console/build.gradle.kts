plugins {
    kotlin("js")
}

kotlin {
    js(IR) {
        browser()
    }
}

dependencies {
    implementation(projects.kotlinw.kotlinwLoggingSpi)

    testImplementation(kotlin("test"))
}
