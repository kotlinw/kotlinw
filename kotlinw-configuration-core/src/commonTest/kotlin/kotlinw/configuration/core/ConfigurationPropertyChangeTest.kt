package kotlinw.configuration.core

import app.cash.turbine.test
import arrow.core.continuations.AtomicRef
import kotlinw.eventbus.local.LocalEventBusImpl
import kotlinw.util.stdlib.Priority
import kotlinw.util.stdlib.concurrent.value
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class ConfigurationPropertyChangeTest {

    @Test
    fun testWatchNonEnumerablePropertyValueSource() = runTest {
        val propertyName = "a"
        val propertyValueHolder = AtomicRef<String?>(null)

        val source = object : ConfigurationPropertySource {

            override fun getPropertyValueOrNull(key: ConfigurationPropertyKey): EncodedConfigurationPropertyValue? =
                if (key.name == propertyName) propertyValueHolder.value else null

            override val priority = Priority.Normal
        }
        val lookup =
            ConfigurationPropertyLookupImpl(listOf(source, ConstantConfigurationPropertySource.of(mapOf("b" to "x"))))
        val eventBus = LocalEventBusImpl()

        coroutineScope {
            val pollingDelayMillis = 100L
            lookup.watchConfigurationProperties(
                this,
                eventBus,
                pollingDelayMillis.milliseconds,
                setOf(ConfigurationPropertyKey("a"), ConfigurationPropertyKey("b"))
            ).test {
                assertEquals(0, currentTime)
                assertEquals(mapOf(ConfigurationPropertyKey("b") to "x"), awaitItem())
                assertEquals(0, currentTime)

                propertyValueHolder.value = "1"

                assertEquals(
                    mapOf(ConfigurationPropertyKey("a") to "1", ConfigurationPropertyKey("b") to "x"),
                    awaitItem()
                )
                assertEquals(pollingDelayMillis, currentTime)
            }
            currentCoroutineContext().cancelChildren()
        }
    }

    @Test
    fun testWatchEnumerablePropertyValueSource() = runTest {
        val propertyName = ConfigurationPropertyKey("a")
        val propertyValueHolder = AtomicRef<String?>(null)

        val source = object : EnumerableConfigurationPropertySource {

            override val priority = Priority.Normal

            override fun getPropertyKeys() = setOf(propertyName)

            override fun getPropertyValueOrNull(key: ConfigurationPropertyKey): String? =
                if (key == propertyName) propertyValueHolder.value else null
        }
        val lookup =
            ConfigurationPropertyLookupImpl(listOf(source, ConstantConfigurationPropertySource.of(mapOf("b" to "x"))))
        val eventBus = LocalEventBusImpl()

        coroutineScope {
            lookup.watchConfigurationProperties(
                this,
                eventBus,
                100.milliseconds,
                setOf(ConfigurationPropertyKey("a"), ConfigurationPropertyKey("b"))
            ).test {
                assertEquals(0, currentTime)
                assertEquals(mapOf(ConfigurationPropertyKey("b") to "x"), awaitItem())
                assertEquals(0, currentTime)

                val newValue = "1"
                propertyValueHolder.value = newValue
                eventBus.dispatch(ConfigurationPropertyChangeEvent(propertyName, newValue))

                assertEquals(
                    mapOf(ConfigurationPropertyKey("a") to "1", ConfigurationPropertyKey("b") to "x"),
                    awaitItem()
                )
                assertEquals(0, currentTime)
            }
            currentCoroutineContext().cancelChildren()
        }
    }
}
