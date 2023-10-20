import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

plugins {
    `kotlinw-jvm`
}

dependencies {
    api(projects.kotlinw.module.kotlinwModuleCore)
    api(projects.kotlinw.kotlinwPwaCore)
    implementation(projects.kotlinw.module.kotlinwModuleServerbase)

    implementation(libs.kotlin.bom)

    ksp(libs.koin.ksp.compiler)
}
