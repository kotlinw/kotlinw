import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

plugins {
    `kotlinw-jvm`
}

dependencies {
    api(projects.kotlinw.module.kotlinwModuleCore)
    implementation(projects.kotlinw.module.kotlinwModuleServerbase)

    api(projects.kotlinw.kotlinwPwaCore)

    implementation(libs.kotlin.bom)

    ksp(libs.koin.ksp.compiler)
}
