plugins {
    kotlin("jvm")
    kotlin("plugin.jpa")
    kotlin("plugin.noarg")
    kotlin("plugin.allopen")
}

noArg {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
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
    api(projects.shared.kotlinwUlid)
    api(libs.arrow.core)
}
