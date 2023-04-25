plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    implementation(projects.kotlinw.kotlinwRemotingCore)
    api(project.dependencies.platform(libs.spring.boot.bom))
    api(libs.spring.context)
    api(libs.spring.web)
    implementation(libs.jakarta.annotation.api)
    implementation(libs.kotlinx.coroutines.reactor)

    testImplementation(project.dependencies.platform(libs.spring.boot.bom))
    testImplementation(libs.spring.boot.starter.web)
    testImplementation(libs.spring.boot.starter.webflux)
    testImplementation(projects.kotlinw.kotlinwRemotingProcessorTest)
    testImplementation(projects.kotlinw.kotlinwRemotingClientKtor)
    testImplementation(kotlin("test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.ktor.client.java)
    testImplementation(libs.mockk)
}

kotlin {
    target {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
        }
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
}
