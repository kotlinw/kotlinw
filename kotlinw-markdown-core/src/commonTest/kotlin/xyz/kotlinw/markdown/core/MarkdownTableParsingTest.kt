package xyz.kotlinw.markdown.core

import xyz.kotlinw.markdown.core.MarkdownDocumentElement.InlineLink
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Table
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Table.Cell
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Text
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownTableParsingTest {
    @Test
    fun testTable() {
        assertEquals(
            MarkdownDocumentModel(
                Table(
                    listOf(
                        Cell(Text("Library")),
                        Cell(Text("Details"))
                    ),
                    listOf(
                        listOf(
                            Cell(Text("Ktor")),
                            Cell(
                                InlineLink("https://ktor.io/docs/client.html", "Docs"),
                                Text(".")
                            )
                        ),
                        listOf(
                            Cell(Text("DateTime")),
                            Cell(
                                InlineLink("https://github.com/Kotlin/kotlinx-datetime#readme", "Docs"),
                                Text(".")
                            )
                        ),
                        listOf(
                            Cell(Text("SQLDelight")),
                            Cell(
                                Text("Third-party library. "),
                                InlineLink("https://cashapp.github.io/sqldelight/", "Docs"),
                                Text(".")
                            )
                        )
                    )
                )
            ),
            """
| Library    | Details                                                            |
|------------|--------------------------------------------------------------------| 
| Ktor       | [Docs](https://ktor.io/docs/client.html).                          | 
| DateTime   | [Docs](https://github.com/Kotlin/kotlinx-datetime#readme).         |
| SQLDelight | Third-party library. [Docs](https://cashapp.github.io/sqldelight/).|
            """.trimIndent().parseMarkdownDocument()
        )
    }
}
