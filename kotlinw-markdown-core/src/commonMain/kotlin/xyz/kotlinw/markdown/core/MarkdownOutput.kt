package xyz.kotlinw.markdown.core

import xyz.kotlinw.markdown.core.MarkdownDocumentElement.BlockElement
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.BlockQuote
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.CodeBlock
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.HardLineBreak
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Heading
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.HorizontalRule
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.HorizontalRule.Type.Asterisk
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.HorizontalRule.Type.Hyphen
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.HorizontalRule.Type.Underscore
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.HtmlBlock
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.HtmlTag
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Image
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.InlineCode
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.InlineElement
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.InlineLink
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.LinkDefinition
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.ListItem
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.ListItems
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Paragraph
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Table
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Text
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.TextSpan
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.TextSpan.Style.BoldStyle
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.TextSpan.Style.ItalicStyle

fun MarkdownDocumentModel.toMarkdownText(): String =
    buildString {
        val output = MarkdownDocumentOutputImpl(this)
        childElements.forEachIndexed { index, childElement ->
            output.toMarkdownText(childElement, index > 0)
        }
    }

internal sealed interface MarkdownDocumentOutput {
    fun appendText(text: String)

    fun appendTextAndNewline(text: String)

    fun withIndent(
        indentationText: String,
        firstIndentationText: String = indentationText,
        block: MarkdownDocumentOutput.() -> Unit
    )
}

private fun MarkdownDocumentOutput.toMarkdownText(
    element: InlineElement
) {
    when (element) {
        is HardLineBreak -> {
            appendText("  \n")
        }

        is HtmlTag -> {
            appendText(element.htmlFragment)
        }

        is InlineCode -> {
            appendText("`")
            appendText(element.code)
            appendText("`")
        }

        is Image -> {
            appendText("![")
            element.descriptionElements.forEach { toMarkdownText(it) }
            appendText("](")
            appendText(element.url)
            appendText(")")
        }

        is InlineLink -> {
            if (element.textElements.isEmpty()) {
                appendText(element.target) // Auto-link
            } else {
                appendText("[")
                element.textElements.forEach { toMarkdownText(it) }
                appendText("](")
                appendText(element.target)
                appendText(")")
            }
        }

        is Text -> {
            appendText(element.text)
        }

        is TextSpan -> {
            fun appendStyleMarker() {
                if (element.style != null) {
                    if (element.style.italic == ItalicStyle.Italic) {
                        appendText("_")
                    }
                    if (element.style.bold == BoldStyle.Bold) {
                        appendText("**")
                    }
                }
            }

            appendStyleMarker()
            element.childElements.forEach { toMarkdownText(it) }
            appendStyleMarker()
        }
    }
}

private fun MarkdownDocumentOutput.toMarkdownText(
    element: BlockElement,
    shouldAppendNewlineBeforeBlockElement: Boolean = true
) {
    fun appendNewlineBeforeBlockElement() {
        if (shouldAppendNewlineBeforeBlockElement) {
            appendText("\n")
        }
    }

    when (element) {
        is HorizontalRule -> {
            appendNewlineBeforeBlockElement()

            appendTextAndNewline(
                when (element.type) {
                    Hyphen -> "---"
                    Underscore -> "___"
                    Asterisk -> "***"
                }
            )
        }

        is BlockQuote -> {
            appendNewlineBeforeBlockElement()

            withIndent("> ") {
                element.childElements.forEach { toMarkdownText(it) }
            }
        }

        is CodeBlock -> {
            appendNewlineBeforeBlockElement()

            appendText("```")

            if (element.language != null) {
                appendTextAndNewline(element.language)
            }

            appendTextAndNewline(element.sourceCode)
            appendTextAndNewline("```")
        }

        is Heading -> {
            appendNewlineBeforeBlockElement()

            repeat(element.level) { appendText("#") }
            appendText(" ")
            element.heading.forEach { toMarkdownText(it) }
            appendText("\n")

            element.contents.forEach {
                toMarkdownText(it)
            }
        }

        is HtmlBlock -> {
            appendNewlineBeforeBlockElement()
            appendTextAndNewline(element.htmlFragment)
        }

        is LinkDefinition -> {
            appendNewlineBeforeBlockElement()

            appendText("[")
            appendText(element.label)
            appendText("]: ")

            appendText(element.destination)

            if (element.title != null) {
                appendText(" ")
                appendText(element.title)
            }

            appendText("\n")
        }

        is ListItems -> {
            appendNewlineBeforeBlockElement()

            element.items.forEach {
                toMarkdownText(it)
            }
        }

        is ListItem -> {
            withIndent("  ", "* ") {
                element.contents.forEach { toMarkdownText(it, false) }
            }
        }

        is Paragraph -> {
            appendNewlineBeforeBlockElement()
            element.childElements.forEach { toMarkdownText(it) }
            appendText("\n")
        }

        is Table -> {
            appendNewlineBeforeBlockElement()

            appendText("|")
            element.headers.forEach {
                appendText(" ")
                it.contents.map {
                    toMarkdownText(it)
                }
                appendText(" |")
            }
            appendText("\n")

            appendText("| ")
            element.headers.forEach {
                appendText("--- |")
            }
            appendText("\n")

            element.rows.forEach { row ->
                appendText("| ")
                row.forEach {
                    appendText(" ")
                    it.contents.map {
                        toMarkdownText(it)
                    }
                    appendText(" |")
                }
                appendText("\n")
            }
        }
    }
}

internal class MarkdownDocumentOutputImpl(
    private val builder: StringBuilder,
    private val indentationText: String = "",
    private val firstIndentationText: String = indentationText
) : MarkdownDocumentOutput {
    private var isFirstIndentation = true

    private var currentLineIsNotIndented = true

    private fun appendIndentationIfAtStartOfLine() {
        if (currentLineIsNotIndented && indentationText.isNotEmpty()) {

            if (isFirstIndentation) {
                builder.append(firstIndentationText)
                isFirstIndentation = false
            } else {
                builder.append(indentationText)
            }

            currentLineIsNotIndented = false
        }
    }

    private fun appendTextNotContainingNewline(text: String) {
        check(!text.contains('\n'))

        appendIndentationIfAtStartOfLine()

        if (text.isNotEmpty()) {
            builder.append(text)
            currentLineIsNotIndented = false
        }
    }

    private fun appendNewline() {
        appendIndentationIfAtStartOfLine()

        builder.append("\n")
        currentLineIsNotIndented = true
    }

    override fun appendText(text: String) {
        if (text.isNotEmpty()) {
            if (text.contains('\n')) {
                val segments = text.split('\n')

                appendTextNotContainingNewline(segments.first())
                appendNewline()

                if (segments.size > 1) {
                    for (index in 1 until segments.lastIndex) {
                        appendTextNotContainingNewline(segments[index])
                        appendNewline()
                    }

                    val lastSegment = segments.last()
                    if (lastSegment.isNotEmpty()) {
                        appendTextNotContainingNewline(lastSegment)
                    }
                }
            } else {
                appendTextNotContainingNewline(text)
            }
        }
    }

    override fun appendTextAndNewline(text: String) {
        appendText(text)
        appendText("\n")
    }

    override fun withIndent(
        indentationText: String,
        firstIndentationText: String,
        block: MarkdownDocumentOutput.() -> Unit
    ) {
        if (!currentLineIsNotIndented) {
            appendText("\n")
            currentLineIsNotIndented = true
        }

        block(
            MarkdownDocumentOutputImpl(
                builder,
                this.indentationText + indentationText,
                this.indentationText + firstIndentationText
            )
        )

        if (!currentLineIsNotIndented) {
            appendText("\n")
            currentLineIsNotIndented = true
        }
    }
}
