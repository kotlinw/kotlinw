plugins {
    `kotlinw-jvm`
}

dependencies {
    implementation(projects.kotlinw.kotlinwDiApi)
    implementation(projects.kotlinw.kotlinwKspUtil)
    implementation(projects.kotlinw.kotlinwGraph)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.datetime)

    testImplementation(projects.kotlinw.kotlinwKspTestutil)
    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.logback.classic)
}
