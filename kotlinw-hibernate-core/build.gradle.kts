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
    api(projects.kotlinw.kotlinwHibernateApi)
    api(projects.kotlinw.kotlinwJpaCore)
    api(projects.kotlinw.kotlinwJpaRepository)
    implementation(projects.kotlinw.kotlinwJpaInternal)
    api(projects.kotlinw.kotlinwLoggingPlatform)
    api(projects.kotlinw.kotlinwUlid)
    api(projects.kotlinw.kotlinwUtilSerializationJson)
    api(projects.kotlinw.module.kotlinwModuleApi)
    api(libs.arrow.core)

    testImplementation(libs.kotlin.test)
}
