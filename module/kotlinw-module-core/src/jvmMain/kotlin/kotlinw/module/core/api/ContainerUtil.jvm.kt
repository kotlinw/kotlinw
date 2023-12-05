package kotlinw.module.core.api

import kotlinw.configuration.core.DeploymentMode
import kotlinw.configuration.core.DeploymentMode.Development
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.logging.platform.PlatformLogging

// TODO induláskor:
// SLF4J: A number (2) of logging calls during the initialization phase have been intercepted and are
// SLF4J: now being replayed. These are subject to the filtering rules of the underlying logging system.
// SLF4J: See also https://www.slf4j.org/codes.html#replay

// TODO deploymentMode jöhetne paraméterben?

fun readDeploymentModeFromSystemProperty(defaultDeploymentMode: DeploymentMode = Development): DeploymentMode {
    val deploymentModeFromSystemProperty = System.getProperty("kotlinw.core.deploymentMode")
    return if (deploymentModeFromSystemProperty.isNullOrBlank()) {
        defaultDeploymentMode
    } else {
        DeploymentMode.of(deploymentModeFromSystemProperty) // TODO kisbetűsen is fogadja el
    }.also {
        PlatformLogging.getLogger().info { "Deployment mode: " / it } // TODO egységes loggert
    }
}
