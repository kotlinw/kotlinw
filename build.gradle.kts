import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        // TODO setDefaultRepositories()
        mavenCentral()
        google()
        maven(uri("https://repo.spring.io/release"))
        maven(uri("https://repo.spring.io/milestone"))
        maven(uri("https://maven.pkg.jetbrains.space/public/p/compose/dev"))
        maven(uri("https://androidx.dev/storage/compose-compiler/repository/"))
        maven(uri("https://jitpack.io"))
    }
}

plugins {
    kotlin("multiplatform") version "1.7.10" apply false
    kotlin("plugin.serialization") version "1.7.10" apply false
    id("com.google.devtools.ksp") version "1.7.10-1.0.6" apply false
    id("org.jetbrains.compose") version "1.2.0-alpha01-dev741" apply false
}

subprojects {
    group = "kotlinw"
    version = "1.0.0-SNAPSHOT"

    repositories {
        // TODO setDefaultRepositories()
        mavenCentral()
        google()
        maven(uri("https://repo.spring.io/release"))
        maven(uri("https://repo.spring.io/milestone"))
        maven(uri("https://maven.pkg.jetbrains.space/public/p/compose/dev"))
        maven(uri("https://androidx.dev/storage/compose-compiler/repository/"))
        maven(uri("https://jitpack.io"))
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            languageVersion = "1.7"
            apiVersion = "1.7"
            jvmTarget = "17"
            javaParameters = true
            freeCompilerArgs += listOf("-Xjsr305=strict", "-Xopt-in=kotlin.RequiresOptIn", "-Xenable-builder-inference")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

rootProject.plugins.withType<YarnPlugin> {
    rootProject.the<YarnRootExtension>().disableGranularWorkspaces()
}
