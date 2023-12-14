plugins {
    `kotlinw-jvm-library`
}

dependencies {
    api(projects.kotlinw.module.kotlinwModuleWebappCore)
    api(projects.kotlinw.module.kotlinwModuleKtorServer)

    api(projects.kotlinw.kotlinwPwaCore)

    implementation(libs.kotlin.bom)
}
