plugins {
    `kotlinw-multiplatform-library`
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.kotlinw.module.kotlinwModuleAppbase)
                api(projects.kotlinw.module.kotlinwModuleAuthCore)
                api(projects.kotlinw.kotlinwI18nCoreMp)
            }
        }
    }
}
