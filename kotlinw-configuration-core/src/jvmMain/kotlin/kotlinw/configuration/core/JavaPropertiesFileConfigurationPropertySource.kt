package kotlinw.configuration.core

import kotlinw.eventbus.local.LocalEventBus
import kotlinw.util.stdlib.Priority
import kotlinw.util.stdlib.io.FileLocation
import kotlinx.coroutines.CoroutineScope
import java.io.StringReader
import java.lang.reflect.Constructor
import java.util.Properties
import kotlin.time.Duration

class JavaPropertiesFileConfigurationPropertySource private constructor(
    override val priority: Priority,
    private val delegate: DelegatingFileConfigurationPropertySource
) : EnumerableConfigurationPropertySource {

    companion object {

        private val delegateFactory = { contents: String ->
            JavaPropertiesConfigurationPropertySource(
                Properties().also {
                    it.load(StringReader(contents))
                }
            )
        }
    }

    constructor(
        fileLocation: FileLocation,
        priority: Priority = Priority.Normal
    ) :
            this(
                priority,
                DelegatingFileConfigurationPropertySource(
                    fileLocation = fileLocation,
                    delegateFactory = delegateFactory
                )
            )

    constructor(
        fileLocation: FileLocation,
        watcherCoroutineScope: CoroutineScope,
        eventBus: LocalEventBus,
        watchDelay: Duration,
        priority: Priority = Priority.Normal
    ) :
            this(
                priority,
                DelegatingFileConfigurationPropertySource(
                    fileLocation = fileLocation,
                    delegateFactory = delegateFactory,
                    watcherCoroutineScope = watcherCoroutineScope,
                    eventBus = eventBus,
                    watchDelay = watchDelay
                )
            )

    override fun getPropertyKeys() = delegate.getPropertyKeys()

    override fun getPropertyValueOrNull(key: ConfigurationPropertyKey) = delegate.getPropertyValueOrNull(key)
}
