package xyz.kotlinw.markdown.core

import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Heading
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.InlineLink
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Paragraph
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Text
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownHeadingParsingTest {
    @Test
    fun testHeadingWithContent() {
        val sourceDocument = """
            # Heading 1
            
            Some content
        """.trimIndent()
        assertEquals(
            MarkdownDocumentModel(
                Heading(1, "Heading 1", Paragraph("Some content"))
            ),
            sourceDocument.parseMarkdownDocument()
        )
    }

    @Test
    fun testHeadings() {
        val sourceDocument = """
            # Heading 1
            
            # Heading 1
            
            ## Heading 2
            
            ### Heading 3
            
            #### Heading 4
            
            ##### Heading 5
            
            ###### Heading 6
        """.trimIndent()
        assertEquals(
            MarkdownDocumentModel(
                Heading(1, "Heading 1"),
                Heading(
                    1, "Heading 1",
                    Heading(
                        2, "Heading 2",
                        Heading(
                            3, "Heading 3",
                            Heading(
                                4, "Heading 4",
                                Heading(
                                    5, "Heading 5",
                                    Heading(6, "Heading 6")
                                )
                            )
                        )
                    )
                )
            ),
            sourceDocument.parseMarkdownDocument()
        )
    }

    @Test
    fun testSetexts() {
        val sourceDocument = """
            Heading 1
            =========
            
            Heading 2
            ---------
        """.trimIndent()
        assertEquals(
            MarkdownDocumentModel(
                Heading(
                    1, "Heading 1",
                    Heading(2, "Heading 2")
                )
            ),
            sourceDocument.parseMarkdownDocument()
        )
    }

    @Test
    fun testNestedHeadings() {
        val sourceDocument = """
            # Heading 1.1
            
            ## Heading 1.1.1
            
            Contents 1.1.1
            
            ## Heading 1.1.2
            
            Contents 1.1.2
            
            # Heading 1.2

            Contents 1.2.1
        """.trimIndent()
        assertEquals(
            MarkdownDocumentModel(
                Heading(
                    1, "Heading 1.1",
                    Heading(
                        2, "Heading 1.1.1",
                        Paragraph("Contents 1.1.1")
                    ),
                    Heading(
                        2, "Heading 1.1.2",
                        Paragraph("Contents 1.1.2")
                    )
                ),
                Heading(
                    1, "Heading 1.2",
                    Paragraph("Contents 1.2.1")
                )
            ),
            sourceDocument.parseMarkdownDocument()
        )
    }

    @Test
    fun testHeadingTextWithInlineLink() {
        val sourceDocument = """
            # Heading 1 with [inline link](http://www.kotlin.world)
            
            Some content
        """.trimIndent()
        assertEquals(
            MarkdownDocumentModel(
                Heading(
                    1,
                    listOf(
                        Text("Heading 1 with "),
                        InlineLink("http://www.kotlin.world", "inline link")
                    ),
                    listOf(Paragraph("Some content"))
                )
            ),
            sourceDocument.parseMarkdownDocument()
        )
    }
}
