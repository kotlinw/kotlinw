import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

buildscript {
    dependencies {
        // TODO classpath("org.jetbrains.dokka:kotlin-as-java-plugin:1.7.20")
        classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.25.0")
    }
}

plugins {
//    kotlin("multiplatform") version "2.0.10" apply false
//    kotlin("plugin.jpa") version "2.0.10" apply false
//    id("com.google.devtools.ksp") version "2.0.10-1.0.24" apply false
//    kotlin("plugin.serialization") version "2.0.10" apply false
//    kotlin("plugin.spring") version "2.0.10" apply false
//    id("org.jetbrains.compose") version "1.6.11" apply false
    // TODO id("org.jetbrains.dokka") version "1.7.20" apply false
    `maven-publish`
    signing
    // TODO kover: id("org.jetbrains.kotlinx.kover") version "0.8.3"
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.13.1" apply false
}

val isPublicationActive = buildMode == DevelopmentMode.Production

val projectVersion: String by project

val kotlinJavaCompatibility = "11" // TODO make it compatible with Android
println("Kotlin default Java compatibility: $kotlinJavaCompatibility")

val notPublishedProjects = listOf(
    projects.kotlinw.kotlinwRemotingProcessorTest,
    projects.kotlinw.kotlinwDiTest,
    projects.kotlinw.kotlinwImmutatorTest,
    // TODO projects.kotlinw.kotlinwImmutatorExampleWebapp,
    projects.kotlinw.module.kotlinwModuleServerbaseInternalTest
).map { it.dependencyProject }

subprojects {
    group = "xyz.kotlinw"
    version = projectVersion

    if (isPublicationActive) {
        apply(plugin = "signing")
        apply(plugin = "maven-publish")
    }

    apply(plugin = "org.jetbrains.kotlinx.atomicfu")
    // TODO kover: apply(plugin = "org.jetbrains.kotlinx.kover")

    fun KotlinCommonOptions.configureCommonOptions() {
        languageVersion = "1.9"
        apiVersion = "1.9"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlin.time.ExperimentalTime",
            "-opt-in=kotlin.contracts.ExperimentalContracts",
            "-opt-in=kotlin.ExperimentalStdlibApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xenable-builder-inference",
            "-Xcontext-receivers",
        )
    }

    tasks.withType<KotlinCompileCommon> {
        kotlinOptions.configureCommonOptions()
    }
    tasks.withType<KotlinCompile> {
        kotlinOptions {
            configureCommonOptions()
            jvmTarget = kotlinJavaCompatibility
            javaParameters = true
            freeCompilerArgs += listOf(
                "-Xjsr305=strict",
                // "-P", "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=true"
            )
        }
    }
    tasks.withType<Kotlin2JsCompile> {
        kotlinOptions.configureCommonOptions()
    }
    tasks.withType<KotlinNativeCompile> {
        kotlinOptions {
            configureCommonOptions()
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    if (isPublicationActive && project !in notPublishedProjects) {
        val javadocJar by tasks.registering(Jar::class) {
            archiveClassifier.set("javadoc")
        }

        // build.gradle.kts

//region Fix Gradle warning about signing tasks using publishing task outputs without explicit dependencies
// https://github.com/gradle/gradle/issues/26091
        tasks.withType<AbstractPublishToMaven>().configureEach {
            val signingTasks = tasks.withType<Sign>()
            mustRunAfter(signingTasks)
        }
//endregion

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

//    configurations.all {
//        resolutionStrategy {
//            dependencySubstitution {
//                substitute(module("kotlinw:kotlinw-compose")).using(projects.kotlinw.kotlinw-compose)
//                substitute(module("kotlinw:kotlinw-immutator-annotations")).using(projects.kotlinw.kotlinw-immutator-annotations)
//                substitute(module("kotlinw:kotlinw-immutator-api")).using(projects.kotlinw.kotlinw-immutator-api)
//                substitute(module("kotlinw:kotlinw-immutator-example-webapp")).using(projects.kotlinw.kotlinw-immutator-example-webapp)
//                substitute(module("kotlinw:kotlinw-immutator-processor")).using(projects.kotlinw.kotlinw-immutator-processor)
//                substitute(module("kotlinw:kotlinw-immutator-test")).using(projects.kotlinw.kotlinw-immutator-test)
//                substitute(module("kotlinw:kotlinw-immutator-test2")).using(projects.kotlinw.kotlinw-immutator-test2)
//                substitute(module("kotlinw:kotlinw-statemachine-compose")).using(projects.kotlinw.kotlinw-statemachine-compose)
//                substitute(module("kotlinw:kotlinw-statemachine-core")).using(projects.kotlinw.kotlinw-statemachine-core)
//                substitute(module("kotlinw:kotlinw-statemachine-example-desktop")).using(projects.kotlinw.kotlinw-statemachine-example-desktop)
//                substitute(module("kotlinw:kotlinw-statemachine-example-webapp")).using(projects.kotlinw.kotlinw-statemachine-example-webapp)
//                substitute(module("kotlinw:kotlinw-util")).using(projects.kotlinw.kotlinw-util)
//            }
//        }
//    }
}
