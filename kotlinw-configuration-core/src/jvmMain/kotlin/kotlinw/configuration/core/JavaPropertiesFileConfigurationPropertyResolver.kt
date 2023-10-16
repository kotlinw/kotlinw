package kotlinw.configuration.core

import kotlinw.eventbus.local.LocalEventBus
import kotlinx.coroutines.CoroutineScope
import java.io.StringReader
import java.util.Properties
import kotlin.time.Duration
import xyz.kotlinw.io.Resource

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

    constructor(resource: Resource) :
            this(DelegatingFileConfigurationPropertyResolver(resource, delegateFactory))

    constructor(
        resource: Resource,
        watcherCoroutineScope: CoroutineScope,
        eventBus: LocalEventBus,
        watchDelay: Duration
    ) :
            this(
                DelegatingFileConfigurationPropertyResolver(
                    resource = resource,
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
