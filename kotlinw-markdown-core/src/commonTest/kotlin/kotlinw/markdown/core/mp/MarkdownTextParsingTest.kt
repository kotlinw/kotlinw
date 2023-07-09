package kotlinw.markdown.core.mp

import kotlinw.markdown.core.mp.MarkdownDocumentElement.HardLineBreak
import kotlinw.markdown.core.mp.MarkdownDocumentElement.Paragraph
import kotlinw.markdown.core.mp.MarkdownDocumentElement.Text
import kotlinw.markdown.core.mp.MarkdownDocumentElement.TextSpan
import kotlinw.markdown.core.mp.MarkdownDocumentElement.TextSpan.Style
import kotlinw.markdown.core.mp.MarkdownDocumentElement.TextSpan.Style.BoldStyle.Bold
import kotlinw.markdown.core.mp.MarkdownDocumentElement.TextSpan.Style.ItalicStyle.Italic
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownTextParsingTest {
    @Test
    fun testSimpleText() {
        val sourceDocument = "some text"
        assertEquals(
            MarkdownDocumentModel(
                Paragraph(
                    Text("some text")
                )
            ),
            sourceDocument.parseMarkdownDocument()
        )
    }

    @Test
    fun testWrappedText() {
        val sourceDocument =
            """
                First line,
                same line.
            """.trimIndent()
        assertEquals(
            MarkdownDocumentModel(
                Paragraph(
                    Text("First line, same line.")
                )
            ),
            sourceDocument.parseMarkdownDocument()
        )
    }

    @Test
    fun testHardLineBreak() {
        val sourceDocument =
            """
                First line,  
                second line.
            """.trimIndent()
        assertEquals(
            MarkdownDocumentModel(
                Paragraph(
                    Text("First line,"),
                    HardLineBreak(),
                    Text("second line.")
                )
            ),
            sourceDocument.parseMarkdownDocument()
        )
    }

    @Test
    fun testFormattedText() {
        assertEquals(
            MarkdownDocumentModel(
                Paragraph(
                    Text("Just like most modern languages, Kotlin supports single-line (or "),
                    TextSpan(Style(italic = Italic), "end-of-line"),
                    Text(") and multi-line ("),
                    TextSpan(Style(italic = Italic), "block"),
                    Text(") comments."),
                )
            ),
            """
                Just like most modern languages, Kotlin supports single-line (or _end-of-line_) and multi-line (_block_) comments.
            """.trimIndent().parseMarkdownDocument()
        )
    }

    @Test
    fun testTextWithNestedFormatting() {
        assertEquals(
            MarkdownDocumentModel(
                Paragraph(
                    Text("Some text "),
                    TextSpan(
                        Style(italic = Italic),
                        Text("with italic and "),
                        TextSpan(
                            Style(bold = Bold),
                            "bold italic"
                        ),
                        Text(" formatting")
                    ),
                    Text(".")
                )
            ),
            """
                Some text _with italic and **bold italic** formatting_.
            """.trimIndent().parseMarkdownDocument()
        )
    }

    @Test
    fun testEscapingBackticks() {
        // TODO
    }

    @Test
    fun testEscaping() {
        assertEquals(
            MarkdownDocumentModel(
                Paragraph(
                    Text("""some ( text""")
                )
            ),
            """some \( text""".parseMarkdownDocument()
        )
    }
}
