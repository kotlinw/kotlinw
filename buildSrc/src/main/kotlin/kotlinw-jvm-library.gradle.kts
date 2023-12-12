import com.google.devtools.ksp.gradle.KspTaskJvm

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
}

val javaVersion = JavaVersion.VERSION_19

fun JavaVersion.asString() = name.substring(name.lastIndexOf('_') + 1)

fun JavaVersion.asInt() = asString().toInt()

java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

kotlin {
    // TODO jvmToolchain(jdkVersion = javaVersion.asInt())
    target {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, javaVersion.asInt())
        }
        compilations.all {
            kotlinOptions {
                jvmTarget = javaVersion.asString()
            }
        }
    }
}

tasks.withType<KspTaskJvm> {
    kotlinOptions {
        jvmTarget = javaVersion.asString()
    }
}
