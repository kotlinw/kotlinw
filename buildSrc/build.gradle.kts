plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    google()
}

kotlin {
    target {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 11)
        }
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.23")
    implementation("org.jetbrains.kotlin:kotlin-noarg:1.9.23")
    implementation("org.jetbrains.kotlin:kotlin-allopen:1.9.23")
    implementation("org.jetbrains.kotlin:kotlin-serialization:1.9.23")
    implementation("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:1.9.23-1.0.20")
    implementation("com.github.johnrengelman:shadow:8.1.1")
    implementation("org.jetbrains.compose:compose-gradle-plugin:1.6.2")
    // implementation("com.android.tools.build:gradle:8.1.1")
}
