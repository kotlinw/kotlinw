package kotlinw.markdown.core.mp

import kotlinw.markdown.core.mp.MarkdownDocumentElement.InlineLink
import kotlinw.markdown.core.mp.MarkdownDocumentElement.ListItem
import kotlinw.markdown.core.mp.MarkdownDocumentElement.ListItems
import kotlinw.markdown.core.mp.MarkdownDocumentElement.Paragraph
import kotlinw.markdown.core.mp.MarkdownDocumentElement.Table
import kotlinw.markdown.core.mp.MarkdownDocumentElement.Table.Cell
import kotlinw.markdown.core.mp.MarkdownDocumentElement.Text
import kotlinw.markdown.core.mp.MarkdownDocumentElement.TextSpan
import kotlinw.markdown.core.mp.MarkdownDocumentElement.TextSpan.Style
import kotlinw.markdown.core.mp.MarkdownDocumentElement.TextSpan.Style.BoldStyle.Bold
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownListParsingTest {
    @Test
    fun testUnorderedList() {
        val sourceDocument =
            """
                * first item
                * second item
            """.trimIndent()
        assertEquals(
            MarkdownDocumentModel(
                ListItems(
                    false,
                    ListItem(Paragraph("first item")),
                    ListItem(Paragraph("second item")),
                )
            ),
            sourceDocument.parseMarkdownDocument()
        )
    }

    @Test
    fun testOrderedList() {
        val sourceDocument =
            """
                1. first item
                2. second item
            """.trimIndent()
        assertEquals(
            MarkdownDocumentModel(
                ListItems(
                    true,
                    ListItem(Paragraph("first item")),
                    ListItem(Paragraph("second item")),
                )
            ),
            sourceDocument.parseMarkdownDocument()
        )
    }

    @Test
    fun testMultiLineListItem() {
        val sourceDocument =
            """
                * first item 1/2,
                first item 2/2
                * second item
            """.trimIndent()
        assertEquals(
            MarkdownDocumentModel(
                ListItems(
                    false,
                    ListItem(Paragraph("first item 1/2, first item 2/2")),
                    ListItem(Paragraph("second item")),
                )
            ),
            sourceDocument.parseMarkdownDocument()
        )
    }

    @Test
    fun testComplexList() {
        assertEquals(
            MarkdownDocumentModel(
                Paragraph("Here you'll learn how to..."),
                ListItems(
                    true,
                    ListItem(
                        Paragraph(
                            TextSpan(Style(bold = Bold), "Create your first frontend web application:")
                        ),
                        ListItems(
                            false,
                            ListItem(Paragraph("To start from scratch...")),
                            ListItem(Paragraph("If you prefer more robust examples..."))
                        )
                    ),
                    ListItem(
                        Paragraph(
                            TextSpan(Style(bold = Bold), "Use libraries in your application."),
                            Text(" Learn more about "),
                            InlineLink("js-project-setup.md#dependencies", "adding dependencies"),
                            Text("."),
                        ),
                        Table(
                            listOf(Cell(Text("Library")), Cell(Text("Details"))),
                            listOf(
                                listOf(
                                    Cell(Text("stdlib")),
                                    Cell(Text("The Kotlin standard library included in all projects by default."))
                                )
                            )
                        )
                    ),
                    ListItem(
                        Paragraph(
                            TextSpan(Style(bold = Bold), "Learn more about Kotlin for frontend web development:")
                        ),
                        ListItems(
                            false,
                            ListItem(
                                Paragraph(
                                    Text("The "),
                                    InlineLink("js-ir-compiler.md", "new Kotlin/JS IR compiler"),
                                    Text("...")
                                )
                            ),
                            ListItem(
                                Paragraph(
                                    InlineLink("using-packages-from-npm.md", "Using dependencies from npm"),
                                    Text(".")
                                )
                            ),
                            ListItem(
                                Paragraph(
                                    InlineLink("js-to-kotlin-interop.md", "Using Kotlin code from JavaScript"),
                                    Text(".")
                                )
                            ),
                        )
                    ),
                    ListItem(
                        Paragraph(
                            TextSpan(Style(bold = Bold), "Join the Kotlin frontend web community:")
                        ),
                        ListItems(
                            false,
                            ListItem(Paragraph(Text("Slack"))),
                            ListItem(Paragraph(Text("StackOverflow")))
                        )
                    )
                )
            ),
            """
                Here you'll learn how to...
                
                1. **Create your first frontend web application:**
                
                   * To start from scratch...
                   * If you prefer more robust examples...
                
                2. **Use libraries in your application.** Learn more about [adding dependencies](js-project-setup.md#dependencies).  
                    
                   |Library | Details |
                   |--------|---------|
                   |stdlib | The Kotlin standard library included in all projects by default. |
                
                3. **Learn more about Kotlin for frontend web development:**
                
                   * The [new Kotlin/JS IR compiler](js-ir-compiler.md)...
                   * [Using dependencies from npm](using-packages-from-npm.md).
                   * [Using Kotlin code from JavaScript](js-to-kotlin-interop.md).
                
                4. **Join the Kotlin frontend web community:**
                
                   * Slack
                   * StackOverflow
                
            """.trimIndent().parseMarkdownDocument()
        )
    }
}
