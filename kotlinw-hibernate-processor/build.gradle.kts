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
    dependencies {
        implementation(projects.kotlinw.kotlinwHibernateApi)
        implementation(projects.kotlinw.kotlinwKspUtil)
        api(projects.kotlinw.kotlinwDiApi)

        testImplementation(kotlin("test-junit5"))
        testImplementation(libs.logback.classic)
        testImplementation(projects.kotlinw.kotlinwKspTestutil)
    }
}
