pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

enableFeaturePreview("VERSION_CATALOGS")

rootProject.name = "kotlinw-parent"

//include(":lib:kotlinw:kotlinw-compose")

include(":kotlinw:kotlinw-immutator-annotations")
//include(":lib:kotlinw:kotlinw-immutator-api")
//include(":lib:kotlinw:kotlinw-immutator-example-webapp")
//include(":lib:kotlinw:kotlinw-immutator-processor")
//include(":lib:kotlinw:kotlinw-immutator-test")
//include(":lib:kotlinw:kotlinw-immutator-test2")
//
//include(":kotlinw-statemachine-core")
//include(":kotlinw-statemachine-compose")
//include(":kotlinw-statemachine-example-desktop")
//include(":kotlinw-statemachine-example-webapp")
//
//include(":lib:kotlinw:kotlinw-compose")
include(":kotlinw:kotlinw-util")
