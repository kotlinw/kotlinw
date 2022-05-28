pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

enableFeaturePreview("VERSION_CATALOGS")

include(":kotlinw-immutator-api")
include(":kotlinw-immutator-processor")
include(":kotlinw-immutator-test")
