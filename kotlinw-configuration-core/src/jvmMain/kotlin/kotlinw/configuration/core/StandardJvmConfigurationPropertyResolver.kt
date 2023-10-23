package kotlinw.configuration.core

import kotlin.time.Duration
import kotlinw.eventbus.local.LocalEventBus
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.logging.platform.PlatformLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import xyz.kotlinw.io.AbsolutePath
import xyz.kotlinw.io.ClasspathResource
import xyz.kotlinw.io.FileLocation
import xyz.kotlinw.io.FileSystemResource

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
                val fileResourceFromSystemProperty =
                    FileSystemResource(FileLocation(Path(pathStringFromSystemProperty)))
                add(
                    if (watchLocalFiles) {
                        JavaPropertiesFileConfigurationPropertyResolver(
                            fileResourceFromSystemProperty,
                            watcherCoroutineScope!!,
                            eventBus!!,
                            watchDelay!!
                        )
                    } else {
                        JavaPropertiesFileConfigurationPropertyResolver(fileResourceFromSystemProperty)
                    }
                )
            } else {
                logger.debug { "System property not found: $configurationFilePathSystemPropertyName" }
            }

            when (deploymentMode) {
                DeploymentMode.Development ->
                    add(
                        JavaPropertiesFileConfigurationPropertyResolver(
                            ClasspathResource(AbsolutePath("kotlinw-dev.properties"), classLoader)
                        )
                    )

                DeploymentMode.Production ->
                    add(
                        JavaPropertiesFileConfigurationPropertyResolver(
                            ClasspathResource(AbsolutePath("kotlinw-prod.properties"), classLoader)
                        )
                    )
            }

            add(
                JavaPropertiesFileConfigurationPropertyResolver(
                    ClasspathResource(AbsolutePath("kotlinw.properties"), classLoader)
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
