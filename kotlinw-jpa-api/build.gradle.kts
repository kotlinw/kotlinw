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
    api("jakarta.persistence:jakarta.persistence-api:3.1.0") {
        exclude(group = "xml-apis", module = "xml-apis")
    }
    api(libs.arrow.core)
}
