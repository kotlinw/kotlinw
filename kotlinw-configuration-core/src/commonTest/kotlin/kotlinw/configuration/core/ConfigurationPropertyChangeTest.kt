package kotlinw.configuration.core

import app.cash.turbine.test
import arrow.atomic.Atomic
import arrow.atomic.value
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlinw.logging.platform.PlatformLogging
import kotlinw.util.stdlib.Priority
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import xyz.kotlinw.eventbus.inprocess.InProcessEventBus

class ConfigurationPropertyChangeTest {

    @Test
    fun testWatchNonEnumerablePropertyValueSource() = runTest {
        val propertyName = "a"
        val propertyValueHolder = Atomic<String?>(null)

        val source = object : ConfigurationPropertyLookupSource {

            override suspend fun initialize(
                configurationChangeNotifier: ConfigurationChangeNotifier,
                snapshotConfigurationPropertyLookup: SnapshotConfigurationPropertyLookup
            ) {
            }

            override suspend fun reload() {}

            override fun getPropertyValueOrNull(key: ConfigurationPropertyKey): EncodedConfigurationPropertyValue? =
                if (key.name == propertyName) propertyValueHolder.value else null

            override val priority = Priority.Normal
        }
        val lookup =
            ConfigurationPropertyLookupImpl(
                PlatformLogging,
                source,
                ConstantConfigurationPropertyResolver.of(mapOf("b" to "x")).asConfigurationPropertySource()
            )
        val eventBus = InProcessEventBus<ConfigurationEvent>()

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
        val propertyValueHolder = Atomic<String?>(null)

        val source = object : EnumerableConfigurationPropertyLookupSource {

            override val priority = Priority.Normal

            override suspend fun initialize(
                configurationChangeNotifier: ConfigurationChangeNotifier,
                snapshotConfigurationPropertyLookup: SnapshotConfigurationPropertyLookup
            ) {
            }

            override suspend fun reload() {}

            override fun getPropertyKeys() = setOf(propertyName)

            override fun getPropertyValueOrNull(key: ConfigurationPropertyKey): String? =
                if (key == propertyName) propertyValueHolder.value else null
        }
        val lookup =
            ConfigurationPropertyLookupImpl(
                PlatformLogging,
                source,
                ConstantConfigurationPropertyResolver.of(mapOf("b" to "x")).asConfigurationPropertySource()
            )
        val eventBus = InProcessEventBus<ConfigurationEvent>()

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
                eventBus.publish(ConfigurationPropertyChangeEvent(propertyName, newValue))

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
