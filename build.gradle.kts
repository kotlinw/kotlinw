import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

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
    id("org.jetbrains.compose") version "1.2.0-beta02" apply false
    id("org.jetbrains.dokka") version "1.7.10" apply false
    `maven-publish`
    signing
}

subprojects {
    group = "xyz.kotlinw"
    version = "0.0.1-SNAPSHOT"

    apply(plugin = "signing")
    apply(plugin = "maven-publish")

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

    val javadocJar by tasks.registering(Jar::class) {
        archiveClassifier.set("javadoc")
    }

    ext["signing.keyId"] = null
    ext["signing.password"] = null
    ext["signing.secretKeyRingFile"] = null
    ext["ossrhUsername"] = null
    ext["ossrhPassword"] = null
    val secretPropsFile = project.rootProject.file("local.properties")
    if (secretPropsFile.exists()) {
        secretPropsFile.reader().use {
            Properties().apply {
                load(it)
            }
        }.onEach { (name, value) ->
            ext[name.toString()] = value
        }
    } else {
        ext["signing.keyId"] = System.getenv("SIGNING_KEY_ID")
        ext["signing.password"] = System.getenv("SIGNING_PASSWORD")
        ext["signing.secretKeyRingFile"] = System.getenv("SIGNING_SECRET_KEY_RING_FILE")
        ext["ossrhUsername"] = System.getenv("OSSRH_USERNAME")
        ext["ossrhPassword"] = System.getenv("OSSRH_PASSWORD")
    }

    fun getExtraString(name: String) = ext[name]?.toString()

    publishing {
        // Configure maven central repository
        repositories {
            maven {
                name = "sonatype"
                setUrl("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = getExtraString("ossrhUsername")
                    password = getExtraString("ossrhPassword")
                }
            }
        }

        // Configure all publications
        publications.withType<MavenPublication> {

            // Stub javadoc.jar artifact
            artifact(javadocJar.get())

            // Provide artifacts information requited by Maven Central
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
}

rootProject.plugins.withType<YarnPlugin> {
    rootProject.the<YarnRootExtension>().disableGranularWorkspaces()
}
