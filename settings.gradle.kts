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

include(":kotlinw-utils")
