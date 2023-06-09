plugins {
    kotlin("jvm")
}

kotlin {
    target {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 19)
        }
        compilations.all {
            kotlinOptions {
                jvmTarget = "19"
            }
        }
    }
}

dependencies {
    api(projects.kotlinw.kotlinwKspUtil)
    api(libs.tschuchortdev.compiletesting.ksp)
    api(libs.kotlin.test)
}
