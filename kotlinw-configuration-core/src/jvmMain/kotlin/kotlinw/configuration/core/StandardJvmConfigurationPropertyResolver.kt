package kotlinw.configuration.core

import kotlin.time.Duration
import kotlinw.logging.api.LoggerFactory
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.io.files.Path
import xyz.kotlinw.eventbus.inprocess.InProcessEventBus
import xyz.kotlinw.io.AbsolutePath
import xyz.kotlinw.io.ClasspathLocation
import xyz.kotlinw.io.ClasspathResource
import xyz.kotlinw.io.ClasspathScanner
import xyz.kotlinw.io.FileLocation
import xyz.kotlinw.io.FileSystemResource

class StandardJvmConfigurationPropertyResolver
private constructor(
    loggerFactory: LoggerFactory,
    private val classpathScanner: ClasspathScanner,
    private val deploymentMode: DeploymentMode,
    private val classLoader: ClassLoader,
    watchLocalFiles: Boolean,
    watcherCoroutineScope: CoroutineScope?,
    eventBus: InProcessEventBus?,
    watchDelay: Duration?
) : EnumerableConfigurationPropertyResolver {

    companion object {

        const val configurationFilePathSystemPropertyName = "kotlinw.configuration.file"
    }

    private val logger = loggerFactory.getLogger()

    constructor(
        loggerFactory: LoggerFactory,
        classpathScanner: ClasspathScanner,
        deploymentMode: DeploymentMode,
        classLoader: ClassLoader
    ) :
            this(loggerFactory, classpathScanner, deploymentMode, classLoader, false, null, null, null)

    constructor(
        loggerFactory: LoggerFactory,
        classpathScanner: ClasspathScanner,
        deploymentMode: DeploymentMode,
        classLoader: ClassLoader,
        watcherCoroutineScope: CoroutineScope,
        eventBus: InProcessEventBus,
        watchDelay: Duration
    ) : this(
        loggerFactory,
        classpathScanner,
        deploymentMode,
        classLoader,
        true,
        watcherCoroutineScope,
        eventBus,
        watchDelay
    )

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

            add(
                JavaPropertiesFileConfigurationPropertyResolver(
                    ClasspathResource(
                        classpathScanner,
                        ClasspathLocation(AbsolutePath("kotlinw-${deploymentMode.shortName}.properties")),
                        classLoader
                    )
                )
            )

            add(
                JavaPropertiesFileConfigurationPropertyResolver(
                    ClasspathResource(
                        classpathScanner,
                        ClasspathLocation(AbsolutePath("kotlinw.properties")),
                        classLoader
                    )
                )
            )
        }
    )

    override suspend fun reload() {
        delegate.reload()
    }

    override fun getPropertyKeys() = delegate.getPropertyKeys()

    override fun getPropertyValueOrNull(key: ConfigurationPropertyKey) = delegate.getPropertyValueOrNull(key)

    override fun toString(): String {
        return "StandardJvmConfigurationPropertyResolver(delegate=$delegate)"
    }
}
