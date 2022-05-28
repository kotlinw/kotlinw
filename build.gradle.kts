import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        setDefaultRepositories()
    }
}

plugins {
    kotlin("multiplatform") version "1.6.10" apply false
    kotlin("plugin.serialization") version "1.6.10" apply false
}

subprojects {
    group = "kotlinw"
    version = "1.0.0-SNAPSHOT"

    repositories {
        setDefaultRepositories()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            languageVersion = "1.6"
            apiVersion = "1.6"
            jvmTarget = "17"
            javaParameters = true
            freeCompilerArgs += listOf("-Xjsr305=strict", "-Xopt-in=kotlin.RequiresOptIn", "-Xcontext-receivers", "-Xenable-builder-inference")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

rootProject.plugins.withType<YarnPlugin> {
    rootProject.the<YarnRootExtension>().disableGranularWorkspaces()
}
