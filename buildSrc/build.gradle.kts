plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

kotlin {
    target {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 19)
        }
        compilations.all {
            kotlinOptions {
                jvmTarget = "19"
            }
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_19
    targetCompatibility = JavaVersion.VERSION_19
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    implementation("org.jetbrains.kotlin:kotlin-noarg:1.9.22")
    implementation("org.jetbrains.kotlin:kotlin-allopen:1.9.22")
    implementation("org.jetbrains.kotlin:kotlin-serialization:1.9.22")
    implementation("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:1.9.22-1.0.17")
    implementation("com.github.johnrengelman:shadow:8.1.1")
}
