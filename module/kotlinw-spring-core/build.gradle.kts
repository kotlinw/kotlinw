plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    api(projects.kotlinw.module.kotlinwModuleCore)
    api(project.dependencies.platform(libs.spring.boot.bom))
    api(libs.spring.context)
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
