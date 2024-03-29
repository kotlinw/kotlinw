import org.jetbrains.compose.compose

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
    jvm { }
    js(IR) {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.kotlinw.kotlinwUtilCoreMp)
                api(projects.shared.kotlinwRemotingServer)
                api(compose.runtime)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                api(compose.desktop.currentOs)
                api(compose.foundation)
                api(compose.material)
                api(compose.preview)
                api(libs.jetbrains.compose.material.materialiconsextended)
                api(libs.jetbrains.compose.components.desktop.splitpane)
                api("ca.gosyer:compose-material-dialogs-core:0.7.0")
                api("ca.gosyer:compose-material-dialogs-datetime:0.7.0")
                api(libs.devsrsouza.compose.icons.simpleicons)
                api(libs.devsrsouza.compose.icons.feather)
                api(libs.devsrsouza.compose.icons.tablericons)
                api(libs.devsrsouza.compose.icons.evaicons)
                api(libs.devsrsouza.compose.icons.fontawesome)
                api(libs.devsrsouza.compose.icons.octicons)
                api(libs.devsrsouza.compose.icons.linea)
                api(libs.devsrsouza.compose.icons.lineawesome)
                api(libs.devsrsouza.compose.icons.erikflowersweathericons)
                api(libs.devsrsouza.compose.icons.cssgg)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation(libs.logback.classic)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(libs.jetbrains.compose.html.core.js)
                implementation(compose.html.core)
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}
