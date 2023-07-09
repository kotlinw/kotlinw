package kotlinw.markdown.core.mp

import androidx.compose.runtime.Immutable
import kotlinw.markdown.core.mp.MarkdownDocumentElement.InlineElement
import kotlinw.markdown.core.mp.MarkdownDocumentElement.Text
import kotlinw.markdown.core.mp.MarkdownDocumentElement.TextSpan
import kotlinw.markdown.core.mp.MarkdownDocumentElement.TextSpan.Style
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// TODO immutator

@Serializable
@Immutable
sealed class MarkdownDocumentElement {

    abstract val metadata: MarkdownMetadata?

    abstract fun copyWithMetadata(metadata: MarkdownMetadata): MarkdownDocumentElement

    @Serializable
    @Immutable
    sealed class BlockElement : MarkdownDocumentElement()

    @Serializable
    @SerialName("Paragraph")
    @Immutable
    data class Paragraph(
        val childElements: List<InlineElement>,
        override val metadata: MarkdownMetadata? = null
    ) : BlockElement() {
        constructor(vararg childElements: InlineElement) : this(childElements.toList(), null)

        constructor(text: String) : this(Text(text))

        override fun copyWithMetadata(metadata: MarkdownMetadata) = copy(metadata = metadata)
    }

    @Serializable
    @SerialName("List")
    @Immutable
    data class ListItems(
        val isOrdered: Boolean,
        val items: List<ListItem>,
        override val metadata: MarkdownMetadata? = null
    ) : BlockElement() {
        constructor(isOrdered: Boolean, vararg items: ListItem) : this(isOrdered, items.toList(), null)

        override fun copyWithMetadata(metadata: MarkdownMetadata) = copy(metadata = metadata)
    }

    @Serializable
    @SerialName("ListItem")
    @Immutable
    data class ListItem(
        val contents: List<BlockElement>,
        override val metadata: MarkdownMetadata? = null
    ) : BlockElement() {
        constructor(vararg contents: BlockElement) : this(contents.toList(), null)

        override fun copyWithMetadata(metadata: MarkdownMetadata) = copy(metadata = metadata)
    }

    @Serializable
    @SerialName("BlockQuote")
    @Immutable
    data class BlockQuote(
        val childElements: List<BlockElement>,
        override val metadata: MarkdownMetadata? = null
    ) : BlockElement() {
        constructor(vararg childElements: BlockElement) : this(childElements.toList(), null)

        override fun copyWithMetadata(metadata: MarkdownMetadata) = copy(metadata = metadata)
    }

    @Serializable
    @SerialName("CodeBlock")
    @Immutable
    data class CodeBlock(
        val sourceCode: String,
        val language: String? = null,
        override val metadata: MarkdownMetadata? = null
    ) : BlockElement() {
        override fun copyWithMetadata(metadata: MarkdownMetadata) = copy(metadata = metadata)
    }

    // TODO ennek inkább Section legyen a neve
    @Serializable
    @SerialName("Heading")
    @Immutable
    data class Heading(
        val level: Int,
        val heading: List<InlineElement>,
        val contents: List<BlockElement>,
        override val metadata: MarkdownMetadata? = null
    ) : BlockElement() {
        constructor(
            level: Int,
            headingText: String,
            vararg contents: BlockElement
        ) : this(level, listOf(Text(headingText)), contents.toList(), null)

        override fun copyWithMetadata(metadata: MarkdownMetadata) = copy(metadata = metadata)
    }

    @Serializable
    @SerialName("HtmlBlock")
    @Immutable
    data class HtmlBlock(
        val htmlFragment: String,
        override val metadata: MarkdownMetadata? = null
    ) : BlockElement() {
        override fun copyWithMetadata(metadata: MarkdownMetadata) = copy(metadata = metadata)
    }

    @Serializable
    @SerialName("LinkDefinition")
    @Immutable
    data class LinkDefinition(
        val label: String,
        val destination: String,
        val title: String? = null,
        override val metadata: MarkdownMetadata? = null
    ) : BlockElement() {
        val effectiveTitle get() = title ?: label

        override fun copyWithMetadata(metadata: MarkdownMetadata) = copy(metadata = metadata)
    }

    @Serializable
    @SerialName("HorizontalRule")
    @Immutable
    data class HorizontalRule(
        val type: Type,
        override val metadata: MarkdownMetadata? = null
    ) : BlockElement() {
        enum class Type {
            Hyphen, Underscore, Asterisk
        }

        override fun copyWithMetadata(metadata: MarkdownMetadata) = copy(metadata = metadata)
    }

    @Serializable
    @SerialName("Table")
    @Immutable
    data class Table(
        val headers: List<Cell>,
        val rows: List<List<Cell>>,
        override val metadata: MarkdownMetadata? = null
    ) : BlockElement() {

        // TODO replace with TextSpan
        @Serializable
        @SerialName("Cell")
        @Immutable
        data class Cell(val contents: List<InlineElement>) {
            constructor(vararg contents: InlineElement) : this(contents.toList())
        }

        override fun copyWithMetadata(metadata: MarkdownMetadata) = copy(metadata = metadata)
    }

    @Serializable
    @Immutable
    sealed class InlineElement : MarkdownDocumentElement()

    @Serializable
    @SerialName("Image")
    @Immutable
    data class Image(
        val url: String,
        val descriptionElements: List<InlineElement>,
        override val metadata: MarkdownMetadata? = null
    ) : InlineElement() {
        constructor(url: String, description: String): this(url, listOf(Text(description)))

        override fun copyWithMetadata(metadata: MarkdownMetadata) = copy(metadata = metadata)
    }

    @Serializable
    @SerialName("TextSpan")
    @Immutable
    data class TextSpan(
        val style: Style?,
        val childElements: List<InlineElement>,
        override val metadata: MarkdownMetadata? = null
    ) : InlineElement() {

        @Serializable
        @Immutable
        data class Style(
            val italic: ItalicStyle = ItalicStyle.Inherited,
            val bold: BoldStyle = BoldStyle.Inherited
        ) {
            companion object {
                val Inherited = Style()
            }

            enum class ItalicStyle {
                Inherited,
                Italic,
                NonItalic
            }

            enum class BoldStyle {
                Inherited,
                Bold,
                NonBold
            }
        }

        constructor(style: Style? = null, vararg childElements: InlineElement) :
                this(style, childElements.toList())

        constructor(style: Style? = null, text: String) : this(style, Text(text))

        constructor(vararg childElements: InlineElement) :
                this(null, *childElements)

        constructor(text: String) : this(Text(text))

        override fun copyWithMetadata(metadata: MarkdownMetadata) = copy(metadata = metadata)
    }

    @Serializable
    @SerialName("Text")
    @Immutable
    data class Text(
        val text: String,
        override val metadata: MarkdownMetadata? = null
    ) : InlineElement() {
        override fun copyWithMetadata(metadata: MarkdownMetadata) = copy(metadata = metadata)
    }

    @Serializable
    @SerialName("InlineCode")
    @Immutable
    data class InlineCode(
        val code: String,
        override val metadata: MarkdownMetadata? = null
    ) : InlineElement() {
        override fun copyWithMetadata(metadata: MarkdownMetadata) = copy(metadata = metadata)
    }

    @Serializable
    @SerialName("InlineLink")
    @Immutable
    data class InlineLink(
        val target: String,
        val textElements: List<InlineElement> = emptyList(),
        override val metadata: MarkdownMetadata? = null
    ) : InlineElement() {
        constructor(target: String, vararg textElements: InlineElement) : this(target, textElements.toList())

        constructor(target: String, text: String) : this(target, Text(text))

        override fun copyWithMetadata(metadata: MarkdownMetadata) = copy(metadata = metadata)
    }

    @Serializable
    @SerialName("HardLineBreak")
    @Immutable
    data class HardLineBreak(
        override val metadata: MarkdownMetadata? = null
    ) : InlineElement() {
        override fun toString() = this::class.simpleName!!

        override fun copyWithMetadata(metadata: MarkdownMetadata) = copy(metadata = metadata)
    }

    @Serializable
    @SerialName("HtmlTag")
    @Immutable
    data class HtmlTag(
        val htmlFragment: String,
        override val metadata: MarkdownMetadata? = null
    ) : InlineElement() {
        override fun copyWithMetadata(metadata: MarkdownMetadata) = copy(metadata = metadata)
    }
}

@Serializable
@SerialName("MarkdownDocument")
@Immutable
data class MarkdownDocumentModel(
    val childElements: List<BlockElement>,
    override val metadata: MarkdownMetadata? = null
) : MarkdownDocumentElement() {
    constructor(pageMetadata: MarkdownMetadata?, vararg childElements: BlockElement) :
            this(childElements.toList(), pageMetadata)

    constructor(vararg childElements: BlockElement) : this(null, *childElements)

    override fun copyWithMetadata(metadata: MarkdownMetadata) = copy(metadata = metadata)
}

@Serializable
@SerialName("MarkdownMetadata")
@Immutable
data class MarkdownMetadata(val attributes: Map<String, String>) {
    companion object {
        fun of(vararg keyToValue: Pair<String, String>) =
            if (keyToValue.isEmpty()) {
                null
            } else {
                MarkdownMetadata(keyToValue.toMap())
            }
    }

    constructor(vararg nameValuePairs: Pair<String, String>) : this(nameValuePairs.toMap())
}

fun List<InlineElement>.wrapInTextSpanIfNeeded(): InlineElement =
    if (size == 1 && first() is Text) {
        first() as Text
    } else {
        TextSpan(Style.Inherited, this)
    }
