// TODO should be in kotlinw-module-serverbase/commonTest but KSP does not work because of bugs

plugins {
    `kotlinw-jvm-library`
}

dependencies {
    implementation(projects.kotlinw.module.kotlinwModuleServerbase)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)

    kspTest(projects.kotlinw.kotlinwDiProcessor)
}
