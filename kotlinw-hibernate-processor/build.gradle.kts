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
    dependencies {
        implementation(projects.kotlinw.kotlinwHibernateApi)
        implementation(projects.kotlinw.kotlinwKspUtil)

        testImplementation(kotlin("test-junit5"))
        testImplementation(libs.logback.classic)
        testImplementation(projects.kotlinw.kotlinwKspTestutil)
    }
}
