package kotlinw.markdown.core.mp

import kotlinw.markdown.core.mp.MarkdownDocumentElement.Image
import kotlinw.markdown.core.mp.MarkdownDocumentElement.Paragraph
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
