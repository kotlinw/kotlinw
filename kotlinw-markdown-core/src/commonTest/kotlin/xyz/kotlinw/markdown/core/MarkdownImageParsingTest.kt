package xyz.kotlinw.markdown.core

import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Image
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Paragraph
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownImageParsingTest {
    @Test
    fun testImage() {
        assertEquals(
            MarkdownDocumentModel(
                Paragraph(
                    Image("train.jpg", "foo")
                )
            ),
            "![foo](train.jpg)".parseMarkdownDocument()
        )
    }
}
