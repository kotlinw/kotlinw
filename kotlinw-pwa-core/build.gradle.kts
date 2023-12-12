plugins {
    `kotlinw-multiplatform-library`
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.kotlinw.kotlinwIo)
                api(projects.kotlinw.kotlinwUtilStdlibMp)
                api(projects.kotlinw.kotlinwI18nCoreMp)
                api(projects.kotlinw.kotlinwLoggingPlatform)

                api(libs.kotlinx.serialization.core)
                api(libs.kotlinx.io.core)

                implementation(libs.kotlinx.serialization.json)
            }
        }
        jvmMain {
            dependencies {
                implementation(files("$buildDir/resources/"))
            }
        }
    }
}

val jvmProcessResources by tasks.named("jvmProcessResources")

// TODO ehhez van issue valahol felvéve?
val fixMissingResources by tasks.registering(Copy::class) {
    dependsOn(jvmProcessResources)

    from("$buildDir/processedResources/jvm/main")
    into("$buildDir/resources/")
}

val jvmJar by tasks.named("jvmJar") {
    dependsOn(fixMissingResources)
}
