import org.jetbrains.compose.compose

plugins {
    kotlin("multiplatform")
    kotlin("plugin.spring")
    id("com.google.devtools.ksp")
    id("org.jetbrains.compose")
    id("org.springframework.boot")
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
                implementation(project(":kotlinw-immutator-api"))
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
                project.dependencies.platform(libs.spring.boot.bom)
                implementation("org.springframework.boot:spring-boot-starter-web")
                implementation("org.springframework.boot:spring-boot-devtools")
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
    add("kspJs", project(":kotlinw-immutator-processor"))
}
