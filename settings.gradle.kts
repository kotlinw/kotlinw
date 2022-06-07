pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

enableFeaturePreview("VERSION_CATALOGS")

include(":kotlinw-immutator-annotations")
include(":kotlinw-immutator-api")
include(":kotlinw-immutator-example-webapp")
include(":kotlinw-immutator-processor")
include(":kotlinw-immutator-test")
include(":kotlinw-immutator-test2")

include(":kotlinw-statemachine-core")
include(":kotlinw-statemachine-compose")
include(":kotlinw-statemachine-dot-annotation")
include(":kotlinw-statemachine-dot-processor")
include(":kotlinw-statemachine-example-desktop")
include(":kotlinw-statemachine-example-webapp")

include(":kotlinw-compose")
include(":kotlinw-util")
