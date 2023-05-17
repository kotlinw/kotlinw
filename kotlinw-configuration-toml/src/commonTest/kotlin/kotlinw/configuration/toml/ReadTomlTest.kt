package kotlinw.configuration.toml

import kotlinw.configuration.core.ConfigurationPropertyKey
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadTomlTest {

    @Test
    fun testEmpty() {
        assertEquals(emptyMap(), readToml(""))
    }

    @Test
    fun testBareKey() {
        assertEquals(mapOf(ConfigurationPropertyKey("1") to "11"), readToml("""1 = "11""""))
        assertEquals(mapOf(ConfigurationPropertyKey("a") to "aa"), readToml("""a = "aa""""))
        assertEquals(mapOf(ConfigurationPropertyKey("a-b") to "aa-bb"), readToml("""a-b = "aa-bb""""))
    }

    @Test
    fun testQuotedKey() {
        assertEquals(mapOf(ConfigurationPropertyKey("1 and 2") to "1 2"), readToml(""""1 and 2" = "1 2""""))
    }

    @Test
    fun testDottedKey() {
        assertEquals(
            mapOf(
                ConfigurationPropertyKey("name") to "Orange",
                ConfigurationPropertyKey("physical.color") to "orange",
                ConfigurationPropertyKey("physical.shape") to "round",
                ConfigurationPropertyKey("""site."google.com"""") to "true"
            ),
            readToml(
                """
                    name = "Orange"
                    physical.color = "orange"
                    physical.shape = "round"
                    site."google.com" = true
                """.trimIndent()
            )
        )
    }

    @Test
    fun testArray() {
        assertEquals(
            mapOf(
                ConfigurationPropertyKey("integers.0") to "1",
                ConfigurationPropertyKey("integers.1") to "2",
                ConfigurationPropertyKey("integers.2") to "3"
            ),
            readToml("integers = [ 1, 2, 3 ]".trimIndent())
        )
    }

    @Test
    fun testInlineTable() {
        assertEquals(
            mapOf(
                ConfigurationPropertyKey("name.first") to "Tom",
                ConfigurationPropertyKey("name.last") to "Preston-Werner",
                ConfigurationPropertyKey("point.x") to "1",
                ConfigurationPropertyKey("point.y") to "2",
                ConfigurationPropertyKey("animal.type.name") to "pug",
            ),
            readToml(
                """
                    name = { first = "Tom", last = "Preston-Werner" }
                    point = { x = 1, y = 2 }
                    animal = { type.name = "pug" }
                """.trimIndent()
            )
        )
    }

    @Test
    fun testSourceInfo() {
        val properties = readToml(
            """
                    [fruit]
                    apple.color = "red"
                    apple.taste.sweet = true

                    # [fruit.apple]  # INVALID
                    # [fruit.apple.taste]  # INVALID

                    [fruit.apple.texture]  # you can add sub-tables
                    smooth = true
                """.trimIndent(),
            "demo.toml"
        )
        assertEquals(
            mapOf(
                ConfigurationPropertyKey("fruit.apple.color") to "red",
                ConfigurationPropertyKey("fruit.apple.taste.sweet") to "true",
                ConfigurationPropertyKey("fruit.apple.texture.smooth") to "true"
            ),
            properties
        )

        assertEquals("demo.toml@9", properties.keys.first { it.name == "fruit.apple.texture.smooth" }.sourceInfo)
        assertEquals(
            "fruit.apple.texture.smooth (source: demo.toml@9)",
            properties.keys.first { it.name == "fruit.apple.texture.smooth" }.toString()
        )
    }
}
