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
    api(projects.kotlinw.kotlinwHibernateApi)
    api(projects.kotlinw.kotlinwHibernateCore)
    api(projects.kotlinw.module.kotlinwModuleCore)

    implementation(libs.hibernate.graalvm)

    testImplementation(kotlin("test"))
    testImplementation(projects.kotlinw.kotlinwJdbcUtil)
    testImplementation(libs.h2)
}
