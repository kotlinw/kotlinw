plugins {
    kotlin("jvm")
}

kotlin {
    target {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 11)
        }
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }
}

dependencies {
}
