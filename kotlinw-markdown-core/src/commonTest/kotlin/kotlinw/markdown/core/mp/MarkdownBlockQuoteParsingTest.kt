package kotlinw.markdown.core.mp

import kotlinw.markdown.core.mp.MarkdownDocumentElement.BlockQuote
import kotlinw.markdown.core.mp.MarkdownDocumentElement.Paragraph
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownBlockQuoteParsingTest {
    @Test
    fun testBlockQuote() {
        assertEquals(
            MarkdownDocumentModel(
                BlockQuote(
                    Paragraph("Some text, split into 4 lines."),
                )
            ),
            """
                > Some text,
                >split
                > into 4
                > lines.
            """.trimIndent().parseMarkdownDocument()
        )
    }

    @Test
    fun testNestedBlockQuote() {
        assertEquals(
            MarkdownDocumentModel(
                BlockQuote(
                    Paragraph("Outer 1"),
                    BlockQuote(Paragraph("Nested 1 Nested 2")),
                    Paragraph("Outer 2")
                )
            ),
            """
                > Outer 1
                >
                >> Nested 1
                >> Nested 2
                >
                > Outer 2
            """.trimIndent().parseMarkdownDocument()
        )
    }
}
