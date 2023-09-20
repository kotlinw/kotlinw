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
    api(projects.kotlinw.kotlinwJdbcUtil)
    api(libs.hibernate.core)
    api(libs.hibernate.envers)
    api(libs.koin.annotations)
}
