package kotlinw.configuration.core

import kotlinw.eventbus.local.LocalEventBus
import kotlinw.util.stdlib.Priority
import kotlinw.util.stdlib.io.FileLocation
import kotlinx.coroutines.CoroutineScope
import java.io.StringReader
import java.util.Properties
import kotlin.time.Duration

class JavaPropertiesFileConfigurationPropertyResolver private constructor(
    private val delegate: DelegatingFileConfigurationPropertyResolver
) : EnumerableConfigurationPropertyResolver {

    companion object {

        private val delegateFactory = { contents: String ->
            JavaPropertiesConfigurationPropertyResolver(
                Properties().also {
                    it.load(StringReader(contents))
                }
            )
        }
    }

    constructor(fileLocation: FileLocation) :
            this(
                DelegatingFileConfigurationPropertyResolver(
                    fileLocation = fileLocation,
                    delegateFactory = delegateFactory
                )
            )

    constructor(
        fileLocation: FileLocation,
        watcherCoroutineScope: CoroutineScope,
        eventBus: LocalEventBus,
        watchDelay: Duration
    ) :
            this(
                DelegatingFileConfigurationPropertyResolver(
                    fileLocation = fileLocation,
                    delegateFactory = delegateFactory,
                    watcherCoroutineScope = watcherCoroutineScope,
                    eventBus = eventBus,
                    watchDelay = watchDelay
                )
            )

    override suspend fun initialize() {
        delegate.initialize()
    }

    override fun getPropertyKeys() = delegate.getPropertyKeys()

    override fun getPropertyValueOrNull(key: ConfigurationPropertyKey) = delegate.getPropertyValueOrNull(key)
}
