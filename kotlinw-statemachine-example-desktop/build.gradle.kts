import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    id("org.jetbrains.compose")
}

dependencies {
    implementation(projects.kotlinw.kotlinwCompose)
    implementation(projects.kotlinw.kotlinwStatemachineCompose)
    implementation(compose.desktop.currentOs)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material)
    implementation(compose.preview)
}
