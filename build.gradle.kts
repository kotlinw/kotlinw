import kotlinw.project.gradle.DevelopmentMode
import kotlinw.project.gradle.buildMode
import kotlinw.project.gradle.readOssrhAccountData
import kotlinw.project.gradle.readSigningData
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
    dependencies {
        classpath("org.jetbrains.dokka:kotlin-as-java-plugin:1.7.10")
    }
}

plugins {
    kotlin("multiplatform") version "1.7.10" apply false
    kotlin("plugin.serialization") version "1.7.10" apply false
    id("com.google.devtools.ksp") version "1.7.10-1.0.6" apply false
    id("org.jetbrains.compose") version "1.2.0" apply false
    id("org.jetbrains.dokka") version "1.7.10" apply false
    `maven-publish`
    signing
}

val isPublicationActive = buildMode == DevelopmentMode.Production

val projectVersion: String by project

subprojects {
    group = "xyz.kotlinw"
    version = projectVersion

    if (isPublicationActive) {
        apply(plugin = "signing")
        apply(plugin = "maven-publish")
    }

    repositories {
        // TODO setDefaultRepositories()
        mavenCentral()
        google()
        maven(uri("https://repo.spring.io/release"))
        maven(uri("https://repo.spring.io/milestone"))
        maven(uri("https://maven.pkg.jetbrains.space/public/p/compose/dev"))
        maven(uri("https://androidx.dev/storage/compose-compiler/repository/"))
        maven(uri("https://jitpack.io"))
        maven(uri("https://repo.kotlin.link"))
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            languageVersion = "1.7"
            apiVersion = "1.7"
            jvmTarget = "17"
            javaParameters = true
            freeCompilerArgs += listOf("-Xjsr305=strict", "-opt-in=kotlin.RequiresOptIn", "-Xenable-builder-inference")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    if (isPublicationActive) {
        val javadocJar by tasks.registering(Jar::class) {
            archiveClassifier.set("javadoc")
        }

        publishing {
            // Configure maven central repository
            repositories {
                maven {
                    name = "sonatype"
                    setUrl("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                    credentials {
                        readOssrhAccountData().also {
                            username = it.username
                            password = it.password
                        }
                    }
                }
            }

            publications.withType<MavenPublication> {

                artifact(javadocJar.get())

                pom {
                    name.set("kotlinw")
                    description.set("kotlinw - Kotlin Multiplatform library")
                    url.set("https://www.kotlinw.xyz")

                    licenses {
                        license {
                            name.set("MPL")
                            url.set("https://www.mozilla.org/en-US/MPL/2.0/")
                        }
                    }
                    developers {
                        developer {
                            id.set("sandor.norbert")
                            name.set("Norbert Sándor")
                            email.set("sandor.norbert@erinors.com")
                        }
                    }
                    scm {
                        url.set("https://github.com/kotlinw/kotlinw.git")
                    }
                }
            }
        }

        signing {
            readSigningData().also {
                useInMemoryPgpKeys(it.privateKey, it.password)
            }
            sign(publishing.publications)
        }
    }

    configurations.all {
        resolutionStrategy {
            dependencySubstitution {
                substitute(module("kotlinw:kotlinw-util")).using(project(":kotlinw:kotlinw-util"))
                substitute(module("kotlinw:kotlinw-immutator-annotations")).using(project(":kotlinw:kotlinw-immutator-annotations"))
            }
        }
    }
}

rootProject.plugins.withType<YarnPlugin> {
    rootProject.the<YarnRootExtension>().disableGranularWorkspaces()
}
