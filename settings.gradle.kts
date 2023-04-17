pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

enableFeaturePreview("VERSION_CATALOGS")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

//include(":lib:kotlinw:kotlinw-compose")

include(":kotlinw-immutator-annotations")
include(":kotlinw-immutator-graph")
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
include(":kotlinw-util-coroutine-mp")
include(":kotlinw-util-datetime-mp")
include(":kotlinw-util-stdlib-mp")
//include(":kotlinw-tsid")
include(":kotlinw-util")
include(":kotlinw-uuid")
