import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

plugins {
    `kotlinw-jvm-library`
}

dependencies {
    api(projects.kotlinw.module.kotlinwModuleCore)
    api(projects.kotlinw.kotlinwPwaCore)
    implementation(projects.kotlinw.module.kotlinwModuleKtorServer)

    implementation(libs.kotlin.bom)
}
