package kotlinw.configuration.core

import korlibs.io.file.std.JvmClassLoaderResourcesVfs
import korlibs.io.file.std.LocalVfs
import kotlinw.eventbus.local.LocalEventBus
import kotlinx.coroutines.CoroutineScope
import java.io.File
import kotlin.time.Duration
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.logging.platform.PlatformLogging

class StandardJvmConfigurationPropertyResolver
private constructor(
    private val deploymentMode: DeploymentMode,
    private val classLoader: ClassLoader,
    watchLocalFiles: Boolean,
    watcherCoroutineScope: CoroutineScope?,
    eventBus: LocalEventBus?,
    watchDelay: Duration?
) : EnumerableConfigurationPropertyResolver {

    companion object {

        const val configurationFilePathSystemPropertyName = "kotlinw.configuration.file"
    }

    private val logger = PlatformLogging.getLogger()

    constructor(deploymentMode: DeploymentMode, classLoader: ClassLoader) :
            this(deploymentMode, classLoader, false, null, null, null)

    constructor(
        deploymentMode: DeploymentMode,
        classLoader: ClassLoader,
        watcherCoroutineScope: CoroutineScope,
        eventBus: LocalEventBus,
        watchDelay: Duration
    ) : this(deploymentMode, classLoader, true, watcherCoroutineScope, eventBus, watchDelay)

    private val delegate = AggregatingEnumerableConfigurationPropertyResolver(
        buildList {
            val pathStringFromSystemProperty = System.getProperty(configurationFilePathSystemPropertyName)
            if (!pathStringFromSystemProperty.isNullOrBlank()) {
                logger.debug { "Loading configuration from file: " / pathStringFromSystemProperty }
                val fileLocationFromSystemProperty = LocalVfs[pathStringFromSystemProperty]
                add(
                    if (watchLocalFiles) {
                        JavaPropertiesFileConfigurationPropertyResolver(
                            fileLocationFromSystemProperty,
                            watcherCoroutineScope!!,
                            eventBus!!,
                            watchDelay!!
                        )
                    } else {
                        JavaPropertiesFileConfigurationPropertyResolver(fileLocationFromSystemProperty)
                    }
                )
            } else {
                logger.debug { "System property not found: $configurationFilePathSystemPropertyName" }
            }

            when (deploymentMode) {
                DeploymentMode.Development ->
                    add(
                        JavaPropertiesFileConfigurationPropertyResolver(
                            JvmClassLoaderResourcesVfs(classLoader)["/kotlinw-dev.properties"]
                        )
                    )

                DeploymentMode.Production ->
                    add(
                        JavaPropertiesFileConfigurationPropertyResolver(
                            JvmClassLoaderResourcesVfs(classLoader)["/kotlinw-prod.properties"]
                        )
                    )
            }

            add(
                JavaPropertiesFileConfigurationPropertyResolver(
                    JvmClassLoaderResourcesVfs(classLoader)["/kotlinw.properties"]
                )
            )
        }
    )

    override suspend fun initialize() {
        delegate.initialize()
    }

    override fun getPropertyKeys() = delegate.getPropertyKeys()

    override fun getPropertyValueOrNull(key: ConfigurationPropertyKey) = delegate.getPropertyValueOrNull(key)
}
