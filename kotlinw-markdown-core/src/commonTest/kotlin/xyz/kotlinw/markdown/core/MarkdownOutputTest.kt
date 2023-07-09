package xyz.kotlinw.markdown.core

import xyz.kotlinw.markdown.core.MarkdownDocumentElement.ListItem
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.ListItems
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Paragraph
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownOutputTest {
    @Test
    fun testPrintNewline() {
        val builder = StringBuilder()
        val output = MarkdownDocumentOutputImpl(builder)
        output.appendText("\n")
        assertEquals("\n", builder.toString())
    }

    @Test
    fun testPrintNewlines() {
        val builder = StringBuilder()
        val output = MarkdownDocumentOutputImpl(builder)
        output.appendText("\n\n\n")
        assertEquals("\n\n\n", builder.toString())
    }

    @Test
    fun testPrintNewlinesWithIndentation() {
        val builder = StringBuilder()
        val output = MarkdownDocumentOutputImpl(builder, "> ")
        output.appendText("\n\n\n")
        assertEquals("> \n> \n> \n", builder.toString())
    }

    @Test
    fun testPrintWithNewlines() {
        val builder = StringBuilder()
        val output = MarkdownDocumentOutputImpl(builder)
        output.appendText("First line\nSecond line\nThird line\n")
        assertEquals(
            """
                First line
                Second line
                Third line
                
            """.trimIndent(),
            builder.toString()
        )
    }

    @Test
    fun testIndentation() {
        val builder = StringBuilder()
        val output = MarkdownDocumentOutputImpl(builder, "> ")
        output.appendText("First line\nSecond line\nThird line\n")
        assertEquals(
            """
                > First line
                > Second line
                > Third line
                
            """.trimIndent(),
            builder.toString()
        )
    }

    @Test
    fun testNestedIndentation() {
        val builder = StringBuilder()
        with(MarkdownDocumentOutputImpl(builder, "> ")) {
            appendTextAndNewline("1st")
            appendTextAndNewline("2nd")
            withIndent("> ") {
                appendTextAndNewline("1st nested\n2nd nested")
                withIndent("> ") {
                    appendTextAndNewline("very nested")
                }
            }
            appendTextAndNewline("3rd")
        }
        assertEquals(
            """
                > 1st
                > 2nd
                > > 1st nested
                > > 2nd nested
                > > > very nested
                > 3rd
                
            """.trimIndent(),
            builder.toString()
        )
    }

    @Test
    fun testOutputOfList() {
        assertEquals(
            """
                * first item 1/2, first item 2/2
                * second item
                
            """.trimIndent(),

            MarkdownDocumentModel(
                ListItems(
                    false,
                    ListItem(Paragraph("first item 1/2, first item 2/2")),
                    ListItem(Paragraph("second item")),
                )
            ).toMarkdownText()
        )
    }

    @Test
    fun testHeadings() {
        val testMdDocument =
            """
## Other resources

#### Kotlin Foundation

##### [Kotlin Foundation FAQ](https://kotlinfoundation.org/faq/)

            """.trimIndent()
        val model = testMdDocument.parseMarkdownDocument()
        assertEquals(testMdDocument, model.toMarkdownText())
    }
}
