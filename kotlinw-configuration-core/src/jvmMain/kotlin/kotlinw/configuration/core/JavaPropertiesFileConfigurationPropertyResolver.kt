package kotlinw.configuration.core

import xyz.kotlinw.eventbus.inprocess.InProcessEventBus
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
        eventBus: InProcessEventBus<in ConfigurationEvent>,
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

    override suspend fun reload() {
        delegate.reload()
    }

    override fun getPropertyKeys() = delegate.getPropertyKeys()

    override fun getPropertyValueOrNull(key: ConfigurationPropertyKey) = delegate.getPropertyValueOrNull(key)

    override fun toString(): String {
        return "JavaPropertiesFileConfigurationPropertyResolver(delegate=$delegate)"
    }
}
