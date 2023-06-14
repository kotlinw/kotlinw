pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://repo.spring.io/milestone")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
        maven("https://androidx.dev/storage/compose-compiler/repository/")
        maven("https://jitpack.io")
        maven("https://raw.githubusercontent.com/graalvm/native-build-tools/snapshots")
    }
}

dependencyResolutionManagement {
    // repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenCentral()
        google()
        maven("https://repo.spring.io/milestone")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
        maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
        maven("https://androidx.dev/storage/compose-compiler/repository/")
        maven("https://jitpack.io")
        maven("https://repo.kotlin.link")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://repo.gradle.org/gradle/libs-releases")
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "kotlinw"

//include(":lib:kotlinw:kotlinw-compose")

include(":kotlinw-graph")
include(":kotlinw-immutator-annotations")
//include(":lib:kotlinw:kotlinw-immutator-api")
//include(":lib:kotlinw:kotlinw-immutator-example-webapp")
//include(":lib:kotlinw:kotlinw-immutator-processor")
//include(":lib:kotlinw:kotlinw-immutator-test")
//
//include(":kotlinw-statemachine-core")
//include(":kotlinw-statemachine-compose")
//include(":kotlinw-statemachine-example-desktop")
//include(":kotlinw-statemachine-example-webapp")
//
//include(":lib:kotlinw:kotlinw-compose")

include(":kotlinw-ksp-testutil")
include(":kotlinw-ksp-util")

include(":kotlinw-logging-api")
include(":kotlinw-logging-js-console")
include(":kotlinw-logging-jvm-logback")
include(":kotlinw-logging-jvm-slf4j")
include(":kotlinw-logging-platform")
include(":kotlinw-logging-spi")
include(":kotlinw-logging-stdout")
include(":kotlinw-remoting-api")
include(":kotlinw-remoting-core")
include(":kotlinw-remoting-core-ktor")
include(":kotlinw-remoting-client-ktor")
include(":kotlinw-remoting-ipc")
include(":kotlinw-remoting-processor")
include(":kotlinw-remoting-processor-test")
include(":kotlinw-remoting-server-ktor")
include(":kotlinw-remoting-server-spring")
include(":kotlinw-util-coroutine-mp")
include(":kotlinw-util-datetime-mp")
include(":kotlinw-util-stdlib-mp")
//include(":kotlinw-tsid")
include(":kotlinw-util")
include(":kotlinw-uuid")
