package xyz.kotlinw.markdown.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.BlockElement
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Image
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.InlineElement
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.TextSpan.Style.BoldStyle
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.TextSpan.Style.ItalicStyle
import xyz.kotlinw.markdown.core.MarkdownDocumentModel
import xyz.kotlinw.markdown.core.toSingleLineText
import org.jetbrains.compose.web.css.fontStyle
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Code
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.H4
import org.jetbrains.compose.web.dom.H5
import org.jetbrains.compose.web.dom.H6
import org.jetbrains.compose.web.dom.Hr
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Li
import org.jetbrains.compose.web.dom.Ol
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Pre
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Td
import org.jetbrains.compose.web.dom.Th
import org.jetbrains.compose.web.dom.Tr
import org.jetbrains.compose.web.dom.Ul
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.BlockQuote as BlockQuoteElement
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.CodeBlock as CodeBlockElement
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.HardLineBreak as HardLineBreakElement
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Heading as HeadingElement
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.HorizontalRule as HorizontalRuleElement
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.HtmlBlock as HtmlBlockElement
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.HtmlTag as HtmlTagElement
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Image as ImageElement
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.InlineCode as InlineCodeElement
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.InlineLink as InlineLinkElement
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.LinkDefinition as LinkDefinitionElement
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.ListItem as ListItemElement
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.ListItems as ListItemsElement
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Paragraph as ParagraphElement
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Table as TableElement
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Text as TextElement
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.TextSpan as TextSpanElement
import org.jetbrains.compose.web.dom.Table as DomTable
import org.jetbrains.compose.web.dom.Text as DomText

@Composable
actual fun MarkdownDocument(markdownDocument: MarkdownDocumentModel) {
    markdownDocument.childElements.forEach {
        BlockElement(it)
    }
}

@Composable
private fun Paragraph(element: ParagraphElement) {
    P {
        InlineElements(element.childElements)
    }
}

@Composable
private fun Table(element: TableElement) {
    DomTable {
        Tr {
            element.headers.forEach {
                Th {
                    InlineElements(it.contents)
                }
            }
        }

        element.rows.forEach { row ->
            Tr {
                row.forEach {
                    Td {
                        InlineElements(it.contents)
                    }
                }
            }
        }
    }
}

@Composable
private fun ListItems(element: ListItemsElement) {

    @Composable
    fun Items() {
        element.items.forEach {
            Li {
                it.contents.forEach {
                    BlockElement(it)
                }
            }
        }
    }

    if (element.isOrdered) {
        Ol {
            Items()
        }
    } else {
        Ul {
            Items()
        }
    }
}

@Composable
private fun LinkDefinition(element: LinkDefinitionElement) {
    Div {
        DomText("<TODO: LinkDefinition>") // TODO
    }
}

@Composable
private fun HtmlBlock(element: HtmlBlockElement) {
    Div {
        DisposableEffect(element) {
            scopeElement.innerHTML = element.htmlFragment
            onDispose {}
        }
    }
}

@Composable
private fun HorizontalRule(element: HorizontalRuleElement) {
    Hr()
}

@Composable
private fun Heading(element: HeadingElement) {
    with(element) {
        when (level) {
            1 -> Heading1()
            2 -> Heading2()
            3 -> Heading3()
            4 -> Heading4()
            5 -> Heading5()
            6 -> Heading6()
            else -> Heading6()
        }
    }
}

@Composable
private fun HeadingElement.Heading1() {
    H1 {
        HeadingText()
    }
    HeadingContents()
}

@Composable
private fun HeadingElement.Heading2() {
    H2 {
        HeadingText()
    }
    HeadingContents()
}

@Composable
private fun HeadingElement.Heading3() {
    H3 {
        HeadingText()
    }
    HeadingContents()
}

@Composable
private fun HeadingElement.Heading4() {
    H4 {
        HeadingText()
    }
    HeadingContents()
}

@Composable
private fun HeadingElement.Heading5() {
    H5 {
        HeadingText()
    }
    HeadingContents()
}

@Composable
private fun HeadingElement.Heading6() {
    H6 {
        HeadingText()
    }
    HeadingContents()
}

@Composable
private fun HeadingElement.HeadingText() {
    InlineElements(heading)
}

@Composable
private fun HeadingElement.HeadingContents() {
    contents.forEach {
        BlockElement(it)
    }
}

@Composable
private fun CodeBlock(element: CodeBlockElement) {
    P {
        Code {
            Pre {
                DomText(element.sourceCode)
            }
        }
    }
}

@Composable
private fun BlockQuote(element: BlockQuoteElement) {
    // TODO style
    Div {
        element.childElements.forEach {
            BlockElement(it)
        }
    }
}

@Composable
private fun TextSpan(element: TextSpanElement) {
    val appliedStyle = element.style
    if (appliedStyle != null) {
        Span({
            style {
                when (appliedStyle.italic) {
                    ItalicStyle.Inherited -> {}
                    ItalicStyle.Italic -> fontStyle("italic")
                    ItalicStyle.NonItalic -> fontStyle("normal")
                }

                when (appliedStyle.bold) {
                    BoldStyle.Inherited -> {}
                    BoldStyle.Bold -> fontWeight("bold")
                    BoldStyle.NonBold -> fontWeight("normal")
                }
            }
        }) {
            InlineElements(element.childElements)
        }
    } else {
        InlineElements(element.childElements)
    }
}

@Composable
private fun Text(element: TextElement) {
    DomText(element.text)
}

@Composable
private fun InlineLink(element: InlineLinkElement) {
    A(
        href = element.target
    ) {
        InlineElements(element.textElements)
    }
}

@Composable
private fun InlineCode(element: InlineCodeElement) {
    Code {
        DomText(element.code)
    }
}

@Composable
private fun HtmlTag(element: HtmlTagElement) {
    Span {
        DisposableEffect(element) {
            scopeElement.innerHTML = element.htmlFragment
            onDispose {}
        }
    }
}

@Composable
private fun HardLineBreak() {
    Br()
}

@Composable
private fun Image(element: Image) {
    Img(element.url, element.descriptionElements.toSingleLineText().toString())
}

@Composable
private fun BlockElement(element: BlockElement) {
    when (element) {
        is BlockQuoteElement -> BlockQuote(element)
        is CodeBlockElement -> CodeBlock(element)
        is HeadingElement -> Heading(element)
        is HorizontalRuleElement -> HorizontalRule(element)
        is HtmlBlockElement -> HtmlBlock(element)
        is LinkDefinitionElement -> LinkDefinition(element)
        is ListItemsElement -> ListItems(element)
        is ParagraphElement -> Paragraph(element)
        is ListItemElement -> TODO()
        is TableElement -> Table(element)
    }
}

@Composable
fun InlineElement(element: InlineElement) {
    when (element) {
        is HardLineBreakElement -> HardLineBreak()
        is HtmlTagElement -> HtmlTag(element)
        is InlineCodeElement -> InlineCode(element)
        is InlineLinkElement -> InlineLink(element)
        is TextElement -> Text(element)
        is TextSpanElement -> TextSpan(element)
        is ImageElement -> Image(element)
    }
}

@Composable
private fun InlineElements(elements: List<InlineElement>) {
    elements.forEach { InlineElement(it) }
}
