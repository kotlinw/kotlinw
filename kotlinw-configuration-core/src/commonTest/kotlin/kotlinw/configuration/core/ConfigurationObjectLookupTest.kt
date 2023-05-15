package kotlinw.configuration.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromStringMap
import kotlinx.serialization.properties.encodeToStringMap
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConfigurationObjectLookupTest {

    @Serializable
    data class TestData(val a: Int, val b: List<Short> = emptyList())

    // TODO https://github.com/Kotlin/kotlinx.serialization/issues/2302
    @Test
    fun testBug2302() {
        val serialFormat = Properties.Default

        val testData = TestData(1, emptyList())
        val serialized = serialFormat.encodeToStringMap(testData)

        assertEquals(mapOf("a" to "1"), serialized)
        assertEquals(
            testData,
            serialFormat.decodeFromStringMap(serialized)
        )
    }

    @Serializable
    data class TestConfig(val a: Int, val b: String, val c: List<Short> = emptyList())

    @Test
    fun testMissingObject() {
        val configurationPropertyLookup = ConfigurationPropertyLookupImpl(emptyList())
        val configurationObjectLookup: ConfigurationObjectLookup =
            ConfigurationObjectLookupImpl(configurationPropertyLookup)
        assertNull(configurationObjectLookup.getConfigurationObjectOrNull(serializer<TestConfig>()))
        assertNull(configurationObjectLookup.getConfigurationObjectOrNull(serializer<TestConfig>(), "testConfig"))
    }

    @Test
    fun testRootObject() {
        val configurationPropertyLookup = ConfigurationPropertyLookupImpl(
            listOf(
                ConstantConfigurationPropertySource(
                    mapOf("a" to "13", "b" to "abc")
                )
            )
        )
        val configurationObjectLookup = ConfigurationObjectLookupImpl(configurationPropertyLookup)

        assertEquals(
            TestConfig(13, "abc", emptyList()),
            configurationObjectLookup.getConfigurationObjectOrNull(serializer<TestConfig>())
        )
    }
}