plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp")
}

kotlin {
    jvm { }
    js(IR) {
        browser {}
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":kotlinw-immutator-api"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            kotlin.srcDir("build/generated/ksp/jvm/jvmMain/kotlin")
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation(kotlin("reflect"))
            }
        }
        val jvmTest by getting {
            kotlin.srcDir("build/generated/ksp/jvm/jvmTest/kotlin")
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("ch.qos.logback:logback-classic:1.2.5")
            }
        }
        val jsMain by getting {
        }
        val jsTest by getting {
        }
    }
}

dependencies {
//    add("kspMetadata", project(":kotlinw-immutator-processor"))
    add("kspJvm", project(":kotlinw-immutator-processor"))
    add("kspJvmTest", project(":kotlinw-immutator-processor"))
}
