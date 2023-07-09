package kotlinw.markdown.core.mp

import kotlinw.markdown.core.mp.MarkdownDocumentElement.HardLineBreak
import kotlinw.markdown.core.mp.MarkdownDocumentElement.Paragraph
import kotlinw.markdown.core.mp.MarkdownDocumentElement.Text
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownParagraphParsingTest {
    @Test
    fun testMultipleParagraphs() {
        val sourceDocument =
            """
                First paragraph,
                on one line.
                
                Second paragraph, single line.
                
                Third paragraph,  
                with hard line break.
            """.trimIndent()
        assertEquals(
            MarkdownDocumentModel(
                Paragraph(
                    Text("First paragraph, on one line.")
                ),
                Paragraph(
                    Text("Second paragraph, single line.")
                ),
                Paragraph(
                    Text("Third paragraph,"),
                    HardLineBreak(),
                    Text("with hard line break.")
                )
            ),
            sourceDocument.parseMarkdownDocument()
        )
    }
}
