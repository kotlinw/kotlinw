plugins {
    kotlin("jvm")
    kotlin("plugin.jpa")
}

kotlin {
    target {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
        }
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
}

dependencies {
    api(projects.kotlinw.kotlinwHibernateApi)
    api(projects.kotlinw.kotlinwLoggingPlatform)
    api(projects.kotlinw.module.kotlinwModuleApi)
    api(libs.arrow.core)
}
