package kotlinw.configuration.core

import kotlinw.eventbus.local.LocalEventBus
import kotlinw.util.stdlib.Priority
import kotlinw.util.stdlib.io.FileLocation
import kotlinx.coroutines.CoroutineScope
import java.io.StringReader
import java.util.Properties
import kotlin.time.Duration

class JavaPropertiesFileConfigurationPropertySource(
    fileLocation: FileLocation,
    override val priority: Priority = Priority.Normal,
    watcherCoroutineScope: CoroutineScope? = null,
    eventBus: LocalEventBus? = null,
    watchDelay: Duration = Duration.INFINITE
) : EnumerableConfigurationPropertySource {

    private val delegate =
        DelegatingFileConfigurationPropertySource(
            fileLocation = fileLocation,
            delegateFactory = { contents ->
                JavaPropertiesConfigurationPropertySource(Properties().also {
                    it.load(StringReader(contents))
                })
            },
            watcherCoroutineScope = watcherCoroutineScope,
            eventBus = eventBus,
            watchDelay = watchDelay
        )

    override fun getPropertyKeys() = delegate.getPropertyKeys()

    override fun getPropertyValueOrNull(key: ConfigurationPropertyKey) = delegate.getPropertyValueOrNull(key)
}
