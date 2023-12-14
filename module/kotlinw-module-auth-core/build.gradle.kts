plugins {
    `kotlinw-multiplatform-library`
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.kotlinw.module.kotlinwModuleApi)
            }
        }
    }
}
