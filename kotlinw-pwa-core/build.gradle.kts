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
        commonTest {
            dependencies {
                implementation(projects.kotlinw.kotlinwUtilSerializationJson)
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

// TODO https://youtrack.jetbrains.com/issue/KTIJ-16582/Consumer-Kotlin-JVM-library-cannot-access-a-Kotlin-Multiplatform-JVM-target-resources-in-multi-module-Gradle-project
val fixMissingResources by tasks.registering(Copy::class) {
    dependsOn(jvmProcessResources)

    from("$buildDir/processedResources/jvm/main")
    into("$buildDir/resources/")
}

val jvmJar by tasks.named("jvmJar") {
    dependsOn(fixMissingResources)
}
