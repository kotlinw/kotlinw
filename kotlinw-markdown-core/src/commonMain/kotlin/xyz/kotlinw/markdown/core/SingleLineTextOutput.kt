package xyz.kotlinw.markdown.core

import xyz.kotlinw.markdown.core.MarkdownDocumentElement.HardLineBreak
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.HtmlTag
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Image
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.InlineCode
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.InlineElement
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.InlineLink
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Text
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.TextSpan

fun List<InlineElement>.toSingleLineText(): CharSequence {
    val builder = StringBuilder()
    val output = SingleLineTextOutputImpl(builder)
    forEach {
        output.append(it)
    }
    return builder
}

private fun SingleLineTextOutput.append(inlineElements: Iterable<InlineElement>) {
    inlineElements.forEach { append(it) }
}

private fun SingleLineTextOutput.append(inlineElement: InlineElement) {
    when (inlineElement) {
        is HardLineBreak -> appendText(" ")
        is HtmlTag -> appendText(inlineElement.htmlFragment)
        is Image -> append(inlineElement.descriptionElements)
        is InlineCode -> appendText(inlineElement.code)
        is InlineLink -> append(inlineElement.textElements)
        is Text -> appendText(inlineElement.text)
        is TextSpan -> append(inlineElement.childElements)
    }
}

internal sealed interface SingleLineTextOutput {
    fun appendText(text: String)
}

internal class SingleLineTextOutputImpl(
    private val builder: StringBuilder
) : SingleLineTextOutput {
    override fun appendText(text: String) {
        if (text.contains('\n')) {
            builder.append(text.replace('\n', ' '))
        } else {
            builder.append(text)
        }
    }
}
