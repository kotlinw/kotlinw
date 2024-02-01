plugins {
    kotlin("jvm")
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

dependencies {
    implementation(projects.kotlinw.kotlinwRemotingApi)
    implementation(projects.kotlinw.kotlinwKspUtil)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)

    compileOnly(libs.ksp.impl) // TODO temporary
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.9.22") // TODO temporary

    testImplementation(projects.kotlinw.kotlinwKspTestutil)
    testImplementation(libs.arrow.core)
    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.logback.classic)
}
