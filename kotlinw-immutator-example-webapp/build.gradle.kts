import org.jetbrains.compose.compose

plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp")
    id("org.jetbrains.compose")
}

kotlin {
    jvm()
    js(IR) {
        browser {}
        binaries.executable()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":kotlinw:kotlinw-immutator-api"))
                implementation(compose.runtime)
            }
        }
        val jsMain by getting {
            kotlin.srcDir("build/generated/ksp/js/jsMain/kotlin")
            dependencies {
                implementation(compose.web.core)
            }
        }
        val jvmMain by getting {
            dependencies {
            }
        }
    }
}

val jsJar by tasks.getting

val copyGeneratedJsFiles by tasks.registering {
    dependsOn(jsJar)
    copy {
        from(layout.buildDirectory.dir("distributions"))
        into(layout.buildDirectory.dir("processedResources/jvm/main/static"))
    }
}

val jvmProcessResources by tasks.getting
jvmProcessResources.dependsOn(copyGeneratedJsFiles)

dependencies {
    add("kspJs", project(":kotlinw:kotlinw-immutator-processor"))
}
