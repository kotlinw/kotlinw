package kotlinw.markdown.core.mp

import kotlinw.markdown.core.mp.MarkdownDocumentElement.BlockQuote
import kotlinw.markdown.core.mp.MarkdownDocumentElement.CodeBlock
import kotlinw.markdown.core.mp.MarkdownDocumentElement.InlineLink
import kotlinw.markdown.core.mp.MarkdownDocumentElement.ListItem
import kotlinw.markdown.core.mp.MarkdownDocumentElement.ListItems
import kotlinw.markdown.core.mp.MarkdownDocumentElement.Paragraph
import kotlinw.markdown.core.mp.MarkdownDocumentElement.Text
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownMetadataParsingTest {
    private fun assertContentMetadataRegexMatch(expected: Map<String, String>, input: String) {
        assertEquals(expected, extractContentMetadataInfo(input)?.metadata?.attributes)
    }

    @Test
    fun testIsContentMetadataDefinitionText() {
        assertTrue(contentMetadataRegex.matches("""{width="25"}"""))
        assertTrue(contentMetadataRegex.matches("""{width=25}"""))
        assertTrue(contentMetadataRegex.matches("""{width=25}{type="joined"}"""))
        assertTrue(contentMetadataRegex.matches("""{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}"""))
        assertTrue(contentMetadataRegex.matches("""{a="1" b=bbb c=33 d="" e="" f=}"""))
        assertTrue(contentMetadataRegex.matches("""{ a="1"   b=bbb c=33 d="" e="" f=}"""))
    }

    @Test
    fun testIsGroupDefinitionText() {
        assertEquals(
            """width="25"""",
            contentMetadataGroupRegex.matchEntire("""{width="25"}""")?.extractContentMetadataGroup()
        )
        assertEquals(
            """width=25""",
            contentMetadataGroupRegex.matchEntire("""{width=25}""")?.extractContentMetadataGroup()
        )
        assertEquals(
            """kotlin-runnable="true" kotlin-min-compiler-version="1.3"""",
            contentMetadataGroupRegex.matchEntire("""{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}""")
                ?.extractContentMetadataGroup()
        )
        assertEquals(
            """a="1" b=bbb c=33 d="" e="" f=""",
            contentMetadataGroupRegex.matchEntire("""{a="1" b=bbb c=33 d="" e="" f=}""")?.extractContentMetadataGroup()
        )
        assertEquals(
            """a="1"   b=bbb c=33 d="" e="" f=""",
            contentMetadataGroupRegex.matchEntire("""{ a="1"   b=bbb c=33 d="" e="" f=}""")
                ?.extractContentMetadataGroup()
        )
    }

    @Test
    fun testIsKeyValueDefinitionText() {
        assertEquals(
            "width" to "25",
            contentMetadataKeyValueRegex.matchEntire("""width="25"""")?.extractContentMetadataKeyValue()
        )
        assertEquals(
            "width" to "25",
            contentMetadataKeyValueRegex.matchEntire("""width=25""")?.extractContentMetadataKeyValue()
        )
        assertEquals(
            "kotlin-runnable" to "true",
            contentMetadataKeyValueRegex.matchEntire("""kotlin-runnable="true"""")?.extractContentMetadataKeyValue()
        )
        assertEquals(
            "b" to "bbb",
            contentMetadataKeyValueRegex.matchEntire("""b=bbb""")?.extractContentMetadataKeyValue()
        )
        assertEquals(
            "f" to "",
            contentMetadataKeyValueRegex.matchEntire("""f=""")?.extractContentMetadataKeyValue()
        )
    }

    @Test
    fun testSingleAttributeQuoted() {
        assertContentMetadataRegexMatch(
            mapOf("width" to "25"),
            """{width="25"}"""
        )
    }

    @Test
    fun testMultipleAttributesQuoted() {
        assertContentMetadataRegexMatch(
            mapOf("kotlin-runnable" to "true", "kotlin-min-compiler-version" to "1.3"),
            """{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}"""
        )
    }

    @Test
    fun testSingleAttributeUnquoted() {
        assertContentMetadataRegexMatch(
            mapOf("width" to "25"),
            """{width=25}"""
        )
    }

    @Test
    fun testMultipleMetadataBlocks() {
        assertContentMetadataRegexMatch(
            mapOf("width" to "25", "type" to "joined"),
            """{width=25}{type="joined"}"""
        )
    }

    @Test
    fun testPageMetadata() {
        assertEquals(
            "Basic syntax",
            """
                [//]: # (title: Basic syntax)
                
                This is a collection...
            """.trimIndent().parseMarkdownDocument().metadata!!.attributes.getValue("title")
        )
    }

    @Test
    fun testEscapedPageMetadataValue() {
        assertEquals(
            "Functional (SAM) interfaces",
            """
                [//]: # (title: Functional \(SAM\) interfaces)
                
                This is a collection...
            """.trimIndent().parseMarkdownDocument().metadata!!.attributes.getValue("title")
        )
    }

    @Test
    fun testBlockElementMetadata() {
        assertEquals(
            MarkdownDocumentModel(
                CodeBlock(
                    "fun main() {}",
                    "kotlin",
                    MarkdownMetadata("kotlin-runnable" to "true", "kotlin-min-compiler-version" to "1.3")
                )
            ),
            """
                ```kotlin
                fun main() {}
                ```
                {kotlin-runnable="true" kotlin-min-compiler-version="1.3"}
            """.trimIndent().parseMarkdownDocument()
        )
    }

    @Test
    fun testInlineElementMetadata() {
        assertEquals(
            MarkdownDocumentModel(
                Paragraph(
                    Text("before "),
                    InlineLink(
                        "/target.md",
                        listOf(
                            Text("link")
                        ),
                        MarkdownMetadata("wrap" to "false", "color" to "red")
                    ),
                    Text(" after")
                )
            ),
            """
                before [link](/target.md){wrap=false}{color="red"} after
            """.trimIndent().parseMarkdownDocument()
        )
    }

    @Test
    fun testComplexDocumentMetadata() {
        assertEquals(
            MarkdownDocumentModel(
                CodeBlock(
                    "fun main() {}",
                    "kotlin",
                    MarkdownMetadata("kotlin-runnable" to "true", "kotlin-min-compiler-version" to "1.3")
                ),
                ListItems(
                    true,
                    ListItem(
                        Paragraph(
                            InlineLink(
                                "https://link/",
                                listOf(Text("Slack")),
                                MarkdownMetadata("style" to "fancy", "type" to "joined")
                            ),
                            Text(" Slack")
                        )
                    ),
                    ListItem(
                        Paragraph(
                            Text("Second")
                        )
                    ),
                ),
                BlockQuote(
                    listOf(
                        Paragraph(
                            Text("You can also find a multiplatform library in the "),
                            InlineLink(
                                "https://link/",
                                listOf(Text("community-driven list")),
                                MarkdownMetadata("type" to "inlineLink")
                            ),
                            Text(".")
                        )
                    ),
                    MarkdownMetadata("type" to "tip")
                ),
                Paragraph(
                    listOf(Text("Some more text.")),
                    MarkdownMetadata("type" to "paragraph")
                )
            ),
            """
                ```kotlin
                fun main() {}
                ```
                {kotlin-runnable="true" kotlin-min-compiler-version="1.3"}
                
                1. [Slack](https://link/){style=fancy}{type="joined"} Slack

                2. Second

                > You can also find a multiplatform library in the [community-driven list](https://link/){type="inlineLink"}.
                >
                {type="tip"}

                Some more text.
                
                {type="paragraph"}
            """.trimIndent().parseMarkdownDocument()
        )
    }
}
