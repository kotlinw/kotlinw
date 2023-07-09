package xyz.kotlinw.markdown.core

import xyz.kotlinw.markdown.core.MarkdownDocumentElement.InlineCode
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.InlineLink
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.LinkDefinition
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Paragraph
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownLinkParsingTest {
    @Test
    fun testGfmAutoLink() {
        val sourceDocument = "https://pages.github.com/"
        assertEquals(
            MarkdownDocumentModel(
                Paragraph(
                    InlineLink("https://pages.github.com/")
                )
            ),
            sourceDocument.parseMarkdownDocument()
        )
    }

    @Test
    fun testLinkWithLabel() {
        val sourceDocument = "[GitHub Pages](https://pages.github.com/)"
        assertEquals(
            MarkdownDocumentModel(
                Paragraph(
                    InlineLink("https://pages.github.com/", "GitHub Pages")
                )
            ),
            sourceDocument.parseMarkdownDocument()
        )
    }

    @Test
    fun testLinkDefinition() {
        val sourceDocument = "[arbitrary case-insensitive reference text]: https://www.mozilla.org"
        assertEquals(
            MarkdownDocumentModel(
                LinkDefinition("arbitrary case-insensitive reference text", "https://www.mozilla.org")
            ),
            sourceDocument.parseMarkdownDocument()
        )
    }

    @Test
    fun testInlineLinkWithCode() {
        val sourceDocument = "[`let`](scope-functions.md#let)"
        assertEquals(
            MarkdownDocumentModel(
                Paragraph(
                    InlineLink(
                        "scope-functions.md#let",
                        InlineCode("let")
                    )
                )
            ),
            sourceDocument.parseMarkdownDocument()
        )
    }
}
