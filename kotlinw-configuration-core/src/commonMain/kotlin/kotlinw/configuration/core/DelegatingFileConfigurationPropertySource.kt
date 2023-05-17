package kotlinw.configuration.core

import arrow.core.continuations.AtomicRef
import kotlinw.eventbus.local.LocalEventBus
import kotlinw.util.stdlib.Priority
import kotlinw.util.stdlib.concurrent.value
import kotlinw.util.stdlib.io.FileLocation
import kotlinw.util.stdlib.io.readUtf8
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration

class DelegatingFileConfigurationPropertySource private constructor(
    private val fileLocation: FileLocation,
    private val delegateFactory: (String) -> EnumerableConfigurationPropertySource,
    override val priority: Priority = Priority.Normal,
    watcherCoroutineScope: CoroutineScope?,
    eventBus: LocalEventBus?,
    private val watchDelay: Duration?,
    @Suppress("UNUSED_PARAMETER") primaryConstructorMarker: Unit
) : EnumerableConfigurationPropertySource {

    constructor(
        fileLocation: FileLocation,
        delegateFactory: (String) -> EnumerableConfigurationPropertySource,
        priority: Priority = Priority.Normal
    ) :
            this(fileLocation, delegateFactory, priority, null, null, null, Unit)

    constructor(
        fileLocation: FileLocation,
        delegateFactory: (String) -> EnumerableConfigurationPropertySource,
        priority: Priority = Priority.Normal,
        watcherCoroutineScope: CoroutineScope,
        eventBus: LocalEventBus,
        watchDelay: Duration = Duration.INFINITE
    ) :
            this(fileLocation, delegateFactory, priority, watcherCoroutineScope, eventBus, watchDelay, Unit)

    private val delegateHolder = AtomicRef<EnumerableConfigurationPropertySource?>(null)

    private val delegate get() = delegateHolder.value

    init {
        require(watchDelay == null || watchDelay > Duration.ZERO)

        val initialProperties = tryReadConfiguration()
            ?: throw ConfigurationException("Failed to read configuration: ${fileLocation.path}")

        if (watcherCoroutineScope != null && eventBus != null && watchDelay != null) {
            watcherCoroutineScope.launch {
                var previousPropertiesDefinition = initialProperties

                while (true) {
                    delay(watchDelay)

                    val currentPropertiesDefinition = tryReadConfiguration()
                    if (currentPropertiesDefinition != null && currentPropertiesDefinition != previousPropertiesDefinition) {
                        previousPropertiesDefinition = currentPropertiesDefinition
                        delegateHolder.value = delegateFactory(currentPropertiesDefinition)

                        eventBus.dispatch(ConfigurationPropertySourceChangeEvent(this@DelegatingFileConfigurationPropertySource))
                    }
                }
            }
        }
    }

    private fun tryReadConfiguration(): String? =
        try {
            fileLocation.readUtf8()
        } catch (e: Exception) {
            // TODO log
            null
        }

    override fun getPropertyKeys() = delegate?.getPropertyKeys() ?: emptySet()

    override fun getPropertyValueOrNull(key: ConfigurationPropertyKey) = delegate?.getPropertyValueOrNull(key)
}
