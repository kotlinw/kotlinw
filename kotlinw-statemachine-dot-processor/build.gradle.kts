plugins {
    kotlin("jvm")
}

kotlin {
    dependencies {
        implementation(project(":kotlinw-statemachine-dot-annotation"))
        implementation("com.google.devtools.ksp:symbol-processing-api:1.6.21-1.0.5")
        implementation(kotlin("stdlib-jdk8"))

        testImplementation(kotlin("test-junit5"))
        testImplementation("ch.qos.logback:logback-classic:1.2.5")
        testImplementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:1.4.8")
    }
}
