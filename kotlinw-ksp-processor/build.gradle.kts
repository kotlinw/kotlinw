plugins {
    `kotlinw-jvm-library`
}

dependencies {
    implementation(projects.kotlinw.kotlinwDiProcessor)
    implementation(projects.kotlinw.kotlinwKspUtil)

    testImplementation(projects.kotlinw.kotlinwKspTestutil)
    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.logback.classic)
}
