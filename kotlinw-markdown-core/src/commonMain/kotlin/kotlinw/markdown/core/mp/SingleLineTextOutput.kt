package kotlinw.markdown.core.mp

import kotlinw.markdown.core.mp.MarkdownDocumentElement.HardLineBreak
import kotlinw.markdown.core.mp.MarkdownDocumentElement.HtmlTag
import kotlinw.markdown.core.mp.MarkdownDocumentElement.Image
import kotlinw.markdown.core.mp.MarkdownDocumentElement.InlineCode
import kotlinw.markdown.core.mp.MarkdownDocumentElement.InlineElement
import kotlinw.markdown.core.mp.MarkdownDocumentElement.InlineLink
import kotlinw.markdown.core.mp.MarkdownDocumentElement.Text
import kotlinw.markdown.core.mp.MarkdownDocumentElement.TextSpan

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
