plugins {
    kotlin("jvm")
    kotlin("plugin.jpa")
    kotlin("plugin.noarg")
    kotlin("plugin.allopen")
    id("com.google.devtools.ksp")
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
    api(projects.kotlinw.kotlinwHibernateCore)
    api(projects.kotlinw.kotlinwHibernateRepository)
    api(projects.kotlinw.module.kotlinwModuleAppbase) // TODO külön Gradle modulba a schema export-ot, és akkor erre itt nincs szükség
    api(projects.kotlinw.module.kotlinwModuleCore)

    implementation(libs.hibernate.graalvm)

    testImplementation(kotlin("test"))
    testImplementation(projects.kotlinw.kotlinwJdbcUtil)
    testImplementation(libs.h2)
    testImplementation(libs.kotlinx.coroutines.test)

    kspTest(projects.kotlinw.kotlinwDiProcessor)
}
