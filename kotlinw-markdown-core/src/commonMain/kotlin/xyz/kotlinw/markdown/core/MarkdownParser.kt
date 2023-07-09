package xyz.kotlinw.markdown.core

import xyz.kotlinw.markdown.core.MarkdownDocumentElement.BlockElement
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.BlockQuote
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.CodeBlock
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.HardLineBreak
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Heading
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.HorizontalRule
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
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Table.Cell
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Text
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.TextSpan
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.TextSpan.Style
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.TextSpan.Style.BoldStyle
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.TextSpan.Style.ItalicStyle
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.findChildOfType
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.MarkdownFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.flavours.space.SFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

fun String.parseMarkdownDocument(
    macros: Map<String, (ASTNode) -> List<MarkdownDocumentElement>> = emptyMap(),
    flavour: MarkdownFlavourDescriptor = SFMFlavourDescriptor()
): MarkdownDocumentModel =
    MarkdownDocumentParser(this, processPageMetadata(), macros).parse(flavour)

const val invalidMacroPlaceholder = "<invalid-macro>"

private val pageMetadataRegex = Regex("""\[//\]: # \((.+?):\s*(.+?)\)""")

// TODO ez valami extension kellene legyen
// TODO ezzel a megoldással nem lehet makrókat tenni a page metadata-ba
private fun String.processPageMetadata(): MarkdownMetadata? {
    val metadataMap = mutableMapOf<String, String>()

    this.lineSequence()
        .forEach { line ->
            val matchResult = pageMetadataRegex.matchEntire(line)
            if (matchResult != null) {
                val name = matchResult.groupValues[1]
                val value = matchResult.groupValues[2].processMarkdownEscapes().toString()
                metadataMap[name] = value
            }
        }

    return (if (metadataMap.isNotEmpty()) MarkdownMetadata(metadataMap) else null)
}

internal val contentMetadataKeyValueRegex = Regex("""([a-zA-Z][-_a-zA-Z0-9]*)=(?:"([^"]*)"|([^ }]*))""")

// NOTE: Ignore the Idea warning "Redundant character escape '\}' in RegExp" because the escape is required in Javascript.
@Suppress("RegExpRedundantEscape")
internal val contentMetadataGroupRegex = Regex("""\{\s*(?:[a-zA-Z][-_a-zA-Z0-9]*=(?:"[^"]*"|[^ }]*) *)*\}""")

// NOTE: Ignore the Idea warning "Redundant character escape '\}' in RegExp" because the escape is required in Javascript.
@Suppress("RegExpRedundantEscape")
internal val contentMetadataRegex = Regex("""^(?:\{\s*(?:[a-zA-Z][-_a-zA-Z0-9]*=(?:"[^"]*"|[^ }]*) *)*\})+""")

internal fun MatchResult.extractContentMetadataKeyValue(): Pair<String, String> =
    groupValues[1].trim() to (groupValues[2].ifBlank { groupValues[3].trim() })

internal fun MatchResult.extractContentMetadataGroup(): String =
    groupValues[0].let { it.substring(1, it.lastIndex).trim() }

internal data class MarkdownContentMetadataInfo(val metadata: MarkdownMetadata, val metadataDefinitionTextLength: Int)

internal fun extractContentMetadataInfo(input: String): MarkdownContentMetadataInfo? {
    val metadataMatch = contentMetadataRegex.matchAt(input, 0)
    return if (metadataMatch != null) {
        val attributes = mutableMapOf<String, String>()

        var groupMatch = contentMetadataGroupRegex.matchAt(metadataMatch.value, 0)
        while (groupMatch != null) {

            var keyValueMatch = contentMetadataKeyValueRegex.matchAt(groupMatch.extractContentMetadataGroup(), 0)
            while (keyValueMatch != null) {
                attributes += keyValueMatch.extractContentMetadataKeyValue()

                keyValueMatch = keyValueMatch.next()
            }

            groupMatch = groupMatch.next()
        }

        if (attributes.isNotEmpty())
            MarkdownContentMetadataInfo(MarkdownMetadata(attributes), metadataMatch.value.length)
        else
            null
    } else {
        null
    }
}

private fun silentCheck(condition: Boolean) {
    // check(condition) // TODO dev módban működjön
}

private fun silentCheck(checker: () -> Boolean) {
    silentCheck(checker())
}

private val headingElementTypes = setOf(
    MarkdownElementTypes.SETEXT_1,
    MarkdownElementTypes.SETEXT_2,
    MarkdownElementTypes.ATX_1,
    MarkdownElementTypes.ATX_2,
    MarkdownElementTypes.ATX_3,
    MarkdownElementTypes.ATX_4,
    MarkdownElementTypes.ATX_5,
    MarkdownElementTypes.ATX_6
)

private val IElementType.isHeadingElement get() = headingElementTypes.contains(this)

private val ASTNode.isHeadingNode get() = type.isHeadingElement

private val blockElementTypes =
    headingElementTypes +
            setOf(
                MarkdownElementTypes.UNORDERED_LIST,
                MarkdownElementTypes.ORDERED_LIST,
                MarkdownElementTypes.BLOCK_QUOTE,
                MarkdownElementTypes.CODE_FENCE,
                MarkdownElementTypes.CODE_BLOCK,
                MarkdownElementTypes.HTML_BLOCK,
                MarkdownElementTypes.PARAGRAPH,
                MarkdownElementTypes.LINK_DEFINITION,
                MarkdownElementTypes.FULL_REFERENCE_LINK,
                MarkdownElementTypes.SHORT_REFERENCE_LINK,
                MarkdownTokenTypes.HORIZONTAL_RULE,
                GFMElementTypes.TABLE
            )

private val IElementType.isBlockElement get() = blockElementTypes.contains(this)

private val ASTNode.isBlockNode get() = type.isBlockElement

private val inlineElementTypes = setOf(
    MarkdownTokenTypes.BACKTICK,
    MarkdownTokenTypes.ESCAPED_BACKTICKS,
    MarkdownTokenTypes.HTML_TAG,
    MarkdownTokenTypes.SINGLE_QUOTE,
    MarkdownTokenTypes.DOUBLE_QUOTE,
    MarkdownTokenTypes.LPAREN,
    MarkdownTokenTypes.RPAREN,
    MarkdownTokenTypes.LBRACKET,
    MarkdownTokenTypes.RBRACKET,
    MarkdownTokenTypes.LT,
    MarkdownTokenTypes.GT,
    MarkdownTokenTypes.COLON,
    MarkdownTokenTypes.EXCLAMATION_MARK,
    MarkdownTokenTypes.WHITE_SPACE,
    MarkdownTokenTypes.TEXT,
    MarkdownTokenTypes.WHITE_SPACE,
    MarkdownTokenTypes.EOL,
    MarkdownElementTypes.CODE_SPAN,
    MarkdownTokenTypes.HARD_LINE_BREAK,
    GFMTokenTypes.GFM_AUTOLINK,
    MarkdownElementTypes.INLINE_LINK,
    MarkdownElementTypes.SHORT_REFERENCE_LINK,
    MarkdownElementTypes.FULL_REFERENCE_LINK,
    MarkdownElementTypes.STRONG,
    MarkdownElementTypes.EMPH,
    MarkdownElementTypes.IMAGE
)

private val IElementType.isInlineElement get() = inlineElementTypes.contains(this)

private val ASTNode.isInlineNode get() = type.isInlineElement

private const val missingNonOptionalValuePlaceholder = "???"

private class MarkdownDocumentParser(
    private val markdownDocument: String,
    private val pageMetadata: MarkdownMetadata?,
    private val macros: Map<String, (ASTNode) -> List<MarkdownDocumentElement>>
) {
    fun parse(flavour: MarkdownFlavourDescriptor): MarkdownDocumentModel {
        val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(markdownDocument)
        check(parsedTree.type == MarkdownElementTypes.MARKDOWN_FILE) { "Unexpected root element: ${parsedTree.type}" }
        return MarkdownDocumentModel(parsedTree.children.convertBlockContent(parsedTree), pageMetadata)
    }

    private fun ASTNode.getNodeSourceText() = getTextInNode(markdownDocument).toString()

    private val ASTNode.headingLevel
        get() =
            when (type) {
                MarkdownElementTypes.SETEXT_1 -> 1
                MarkdownElementTypes.SETEXT_2 -> 2
                MarkdownElementTypes.ATX_1 -> 1
                MarkdownElementTypes.ATX_2 -> 2
                MarkdownElementTypes.ATX_3 -> 3
                MarkdownElementTypes.ATX_4 -> 4
                MarkdownElementTypes.ATX_5 -> 5
                MarkdownElementTypes.ATX_6 -> 6
                else -> 1
            }

    private val ASTNode.headingContentNode
        get() =
            when (type) {
                MarkdownElementTypes.SETEXT_1,
                MarkdownElementTypes.SETEXT_2 -> MarkdownTokenTypes.SETEXT_CONTENT

                MarkdownElementTypes.ATX_1,
                MarkdownElementTypes.ATX_2,
                MarkdownElementTypes.ATX_3,
                MarkdownElementTypes.ATX_4,
                MarkdownElementTypes.ATX_5,
                MarkdownElementTypes.ATX_6 -> MarkdownTokenTypes.ATX_CONTENT

                else -> null
            }
                ?.let { findChildOfType(it) }

    private interface AstNodeListProcessingContext<E : MarkdownDocumentElement> {
        val nextNode: ASTNode?

        fun consumeNode(): ASTNode

        fun addToResultIfNotNull(element: E?)
    }

    private fun <E : MarkdownDocumentElement> List<ASTNode>.processAstNodes(
        parentNode: ASTNode, // TODO kell?
        process: AstNodeListProcessingContext<E>.() -> Unit
    ): List<E> {
        val processor = AstNodeListProcessor<E>(this)
        processor.context.process()
        return processor.result
    }

    private class AstNodeListProcessor<E : MarkdownDocumentElement>(
        private val nodeList: List<ASTNode>
    ) {
        private val mutableResult: MutableList<E> = ArrayList()

        private var childIndex = -1

        private fun peekNextNode() = if (childIndex + 1 == nodeList.size) null else nodeList[childIndex + 1]

        private var nextNode = peekNextNode()

        val context = object : AstNodeListProcessingContext<E> {
            override val nextNode: ASTNode? get() = this@AstNodeListProcessor.nextNode

            override fun consumeNode(): ASTNode {
                val node = nodeList[++childIndex]
                this@AstNodeListProcessor.nextNode = peekNextNode()
                return node
            }

            private fun attachMetadataToPreviousElement(metadata: MarkdownMetadata) {
                if (mutableResult.isNotEmpty()) {
                    val lastIndex = mutableResult.lastIndex
                    val lastElement = mutableResult[lastIndex]

                    @Suppress("UNCHECKED_CAST")
                    mutableResult[lastIndex] = lastElement.copyWithMetadata(metadata) as E
                } else {
                    // TODO log
                }
            }

            override fun addToResultIfNotNull(element: E?) {
                if (element != null) {
                    // TODO ennek valami extension-nek kellene lenni

                    fun Text.isBlockContentMetadata() = contentMetadataRegex.matches(text)

                    fun Text.isInlineContentMetadata() = contentMetadataRegex.matchesAt(text, 0)

                    fun Text.extractContentMetadataInfo() = extractContentMetadataInfo(text)

                    fun addOriginalElement() {
                        mutableResult.add(element)
                    }

                    if (element is Paragraph) {
                        val isBlockMetadata =
                            element.childElements.size == 1 && (element.childElements.first() as? Text)?.isBlockContentMetadata() ?: false
                        if (isBlockMetadata) {
                            attachMetadataToPreviousElement(
                                (element.childElements.first() as Text).extractContentMetadataInfo()!!.metadata
                            )
                        } else {
                            addOriginalElement()
                        }
                    } else if (element is Text && (mutableResult.isNotEmpty() || nextNode != null) && element.isInlineContentMetadata()) {
                        val metadataInfo = element.extractContentMetadataInfo()!!
                        attachMetadataToPreviousElement(metadataInfo.metadata)

                        val metadataDefinitionTextLength = metadataInfo.metadataDefinitionTextLength
                        if (element.text.length > metadataDefinitionTextLength) {
                            @Suppress("UNCHECKED_CAST")
                            addToResultIfNotNull(Text(element.text.substring(metadataDefinitionTextLength)) as E)
                        }
                    } else {
                        addOriginalElement()
                    }
                }
            }
        }

        val result: List<E> get() = mutableResult
    }

    private fun ASTNode.createErroneousInlineElementReplacement() =
        Text("<invalid node: ${type.name}>")

    private fun List<ASTNode>.convertBlockContent(parentNode: ASTNode): List<BlockElement> =
        processAstNodes(parentNode) {

            fun ASTNode.convertParagraph(): Paragraph? {
                silentCheck(type == MarkdownElementTypes.PARAGRAPH)
                return children.convertInlineContent(this).let {
                    if (it.isNotEmpty()) Paragraph(it) else null
                }
            }

            fun ASTNode.convertListItem(): ListItem? {
                silentCheck(type == MarkdownElementTypes.LIST_ITEM)
                return children
                    .filter { it.type != MarkdownTokenTypes.LIST_NUMBER && it.type != MarkdownTokenTypes.LIST_BULLET } // TODO eltárolni
                    .convertBlockContent(this)
                    .let {
                        if (it.isNotEmpty()) ListItem(it) else null
                    }
            }

            fun ASTNode.convertList(isOrdered: Boolean): ListItems? {
                silentCheck {
                    if (isOrdered) {
                        type == MarkdownElementTypes.ORDERED_LIST
                    } else {
                        type == MarkdownElementTypes.UNORDERED_LIST
                    }
                }
                return children
                    .filter { it.type == MarkdownElementTypes.LIST_ITEM }
                    .mapNotNull { it.convertListItem() }
                    .let {
                        if (it.isNotEmpty()) ListItems(isOrdered, it) else null
                    }
            }

            fun ASTNode.convertBlockQuote(): BlockQuote? {
                silentCheck(type == MarkdownElementTypes.BLOCK_QUOTE)
                return children
                    .convertBlockContent(this)
                    .let { if (it.isNotEmpty()) BlockQuote(it) else null }
            }

            fun ASTNode.convertHtmlBlock(): HtmlBlock? {
                silentCheck(type == MarkdownElementTypes.HTML_BLOCK)
                return children
                    .filter { it.type == MarkdownTokenTypes.HTML_BLOCK_CONTENT }
                    .joinToString("\n") { it.getNodeSourceText() }
                    .let {
                        if (it.isNotBlank()) HtmlBlock(it) else null
                    }
            }

            fun ASTNode.convertLinkDefinition(): LinkDefinition? {
                silentCheck(type == MarkdownElementTypes.LINK_DEFINITION)
                val linkLabel = findChildOfType(MarkdownElementTypes.LINK_LABEL)?.getNodeSourceText()
                val linkDestination = findChildOfType(MarkdownElementTypes.LINK_DESTINATION)?.getNodeSourceText()
                val linkTitle = findChildOfType(MarkdownElementTypes.LINK_TITLE)?.getNodeSourceText()
                return if (linkLabel != null && linkDestination != null) {
                    LinkDefinition(
                        linkLabel
                            .let { if (it.startsWith('[')) it.drop(1) else it }
                            .let { if (it.endsWith(']')) it.dropLast(1) else it },
                        linkDestination,
                        linkTitle
                    )
                } else {
                    null
                }
            }

            fun ASTNode.convertTable(): Table? {

                fun findCells(rowElementType: IElementType) =
                    children
                        .filter { it.type == rowElementType }
                        .map {
                            it.children.filter { it.type == GFMTokenTypes.CELL }
                        }

                val headerCellNodes = findCells(GFMElementTypes.HEADER).first()
                val columnCount = headerCellNodes.size

                fun List<ASTNode>.convertCellContents() =
                    map {
                        Cell(it.children.convertInlineContent(this@convertTable))
                    }

                return if (columnCount > 0) {
                    Table(
                        headerCellNodes.convertCellContents(),
                        findCells(GFMElementTypes.ROW).map { it.take(columnCount).convertCellContents() }
                    )
                } else {
                    null
                }
            }

            fun ASTNode.convertHorizontalRule() =
                HorizontalRule(
                    when (getNodeSourceText().first()) {
                        '-' -> HorizontalRule.Type.Hyphen
                        '_' -> HorizontalRule.Type.Underscore
                        '*' -> HorizontalRule.Type.Asterisk
                        else -> HorizontalRule.Type.Hyphen
                    }
                )

            fun ASTNode.convertCodeLine(): String {
                silentCheck(type == MarkdownTokenTypes.CODE_LINE)
                return getNodeSourceText().let {
                    if (it.startsWith("    ") || it.startsWith('\t')) {
                        it.substring(4)
                    } else {
                        it
                    }
                }
            }

            fun ASTNode.convertCodeBlock(): CodeBlock? {
                silentCheck(type == MarkdownElementTypes.CODE_BLOCK)
                return children.joinToString("") {
                    when (it.type) {
                        MarkdownTokenTypes.CODE_LINE -> it.convertCodeLine()
                        MarkdownTokenTypes.EOL -> "\n"
                        else -> ""
                    }
                }.let {
                    if (it.isNotBlank()) CodeBlock(it) else null
                }
            }

            fun ASTNode.convertCodeFence(): CodeBlock? {
                silentCheck(type == MarkdownElementTypes.CODE_FENCE)

                var language: String? = null

                val code = buildString {
                    var i = 0
                    while (i <= children.lastIndex) {
                        val child = children[i++]
                        val nextChildType = if (i <= children.lastIndex) children[i].type else null
                        when (child.type) {
                            MarkdownTokenTypes.CODE_FENCE_START ->
                                if (nextChildType == MarkdownTokenTypes.EOL) {
                                    i++
                                }

                            MarkdownTokenTypes.FENCE_LANG -> {
                                language = child.getNodeSourceText()
                                if (nextChildType == MarkdownTokenTypes.EOL) {
                                    i++
                                }
                            }

                            MarkdownTokenTypes.CODE_FENCE_CONTENT ->
                                append(child.getNodeSourceText())

                            MarkdownTokenTypes.EOL ->
                                if (nextChildType != MarkdownTokenTypes.CODE_FENCE_END) {
                                    append('\n')
                                }
                        }
                    }
                }

                return if (code.isNotBlank()) CodeBlock(code, language) else null
            }

            fun ASTNode.processNonHeadingNode(): BlockElement? {
                fun createErroneousBlockElementReplacement() = Paragraph(createErroneousInlineElementReplacement())

                return if (isBlockNode) {
                    when (type) {
                        MarkdownElementTypes.UNORDERED_LIST -> convertList(false)
                        MarkdownElementTypes.ORDERED_LIST -> convertList(true)
                        MarkdownElementTypes.BLOCK_QUOTE -> convertBlockQuote()
                        MarkdownElementTypes.CODE_FENCE -> convertCodeFence()
                        MarkdownElementTypes.CODE_BLOCK -> convertCodeBlock()
                        MarkdownElementTypes.HTML_BLOCK -> convertHtmlBlock()
                        MarkdownElementTypes.PARAGRAPH -> convertParagraph()
                        MarkdownElementTypes.LINK_DEFINITION -> convertLinkDefinition()
                        MarkdownElementTypes.FULL_REFERENCE_LINK -> createErroneousBlockElementReplacement() // TODO
                        MarkdownElementTypes.SHORT_REFERENCE_LINK -> createErroneousBlockElementReplacement() // TODO
                        MarkdownTokenTypes.HORIZONTAL_RULE -> convertHorizontalRule()
                        GFMElementTypes.TABLE -> convertTable()
                        else -> createErroneousBlockElementReplacement()
                    }
                } else {
                    null
                }
            }

            fun ASTNode.convertHeading(): Heading {
                val currentHeadingLevel = headingLevel

                val headingTextElements = headingContentNode
                    ?.children
                    ?.dropWhile { it.type == MarkdownTokenTypes.WHITE_SPACE }
                    ?.convertInlineContent(this) ?: listOf(Text("<No title>"))

                val astNodesAfterHeading = parentNode.children.dropWhile { it != this }.drop(1)
                val contents =
                    if (astNodesAfterHeading.isNotEmpty()) {
                        val contentAstNodes = astNodesAfterHeading
                            .takeWhile { !it.isHeadingNode || it.headingLevel > currentHeadingLevel }
                        repeat(contentAstNodes.size) { consumeNode() }
                        contentAstNodes.convertBlockContent(parentNode)
                    } else {
                        emptyList()
                    }

//                val contents = ArrayList<BlockElement>()
//                while (nextNode != null) {
//                    val nextNodeValue = nextNode!!
//                    if (nextNodeValue.isBlockNode) {
//                        if (nextNodeValue.isHeadingNode) {
//                            if (nextNodeValue.headingLevel > currentHeadingLevel) {
//                                consumeNode().convertHeading().let { contents.add(it) }
//                            } else {
//                                break
//                            }
//                        } else {
//                            consumeNode().processNonHeadingNode()?.let { contents.add(it) }
//                        }
//                    } else {
//                        consumeNode() // Ignore non-block element
//                    }
//                }

                return Heading(currentHeadingLevel, headingTextElements, contents)
            }

            while (nextNode != null) {
                val consumedNode = consumeNode()
                addToResultIfNotNull(
                    with(consumedNode) {
                        when (consumedNode.type) {
                            MarkdownElementTypes.SETEXT_1 -> convertHeading()
                            MarkdownElementTypes.SETEXT_2 -> convertHeading()
                            MarkdownElementTypes.ATX_1 -> convertHeading()
                            MarkdownElementTypes.ATX_2 -> convertHeading()
                            MarkdownElementTypes.ATX_3 -> convertHeading()
                            MarkdownElementTypes.ATX_4 -> convertHeading()
                            MarkdownElementTypes.ATX_5 -> convertHeading()
                            MarkdownElementTypes.ATX_6 -> convertHeading()
                            else -> processNonHeadingNode()
                        }
                    }
                )
            }
        }

    private data class InlineLinkData(val linkDestination: String, val inlineElements: List<InlineElement>)

    private fun ASTNode.extractInlineLinkData(): InlineLinkData? {
        val linkDestination = findChildOfType(MarkdownElementTypes.LINK_DESTINATION)?.getNodeSourceText()
        return if (linkDestination != null) {
            InlineLinkData(
                linkDestination,
                findChildOfType(MarkdownElementTypes.LINK_TEXT)
                    ?.children
                    ?.filter { it.type != MarkdownTokenTypes.LBRACKET && it.type != MarkdownTokenTypes.RBRACKET }
                    ?.convertInlineContent(this, true)
                    ?: emptyList()
            )
        } else {
            null
        }
    }

    fun List<ASTNode>.convertInlineContent(parentNode: ASTNode, isRecursiveCall: Boolean = false): List<InlineElement> =
        if (isNotEmpty()) {
            require(groupBy { it.parent }.size == 1) // TODO assert

            processAstNodes(parentNode) {

                fun consumeWhiteSpaceNodes() {
                    while (nextNode != null) {
                        when (nextNode!!.type) {

                            MarkdownTokenTypes.EOL, MarkdownTokenTypes.WHITE_SPACE -> {
                                consumeNode()
                            }

                            else -> break
                        }
                    }
                }

                fun convertTextSpan(): List<InlineElement> {
                    val textSpanElements: MutableList<InlineElement> = ArrayList() // TODO initialCapacity

                    val textBuffer = StringBuilder()

                    // TODO ez valami extension kellene legyen
                    fun processInlineMacro(macroInvocation: String, contextNode: ASTNode): List<InlineElement> {
                        val result = mutableListOf<InlineElement>()
                        val macro = macros[macroInvocation]
                        if (macro != null) {
                            result.addAll(
                                macro(contextNode).filterIsInstance<InlineElement>()
                            )
                        } else {
                            result.add(Text(invalidMacroPlaceholder))
                        }

                        return result
                    }

                    fun consumeTextBuffer() {
                        if (textBuffer.isNotEmpty()) {
                            val text = textBuffer.processMarkdownEscapes().toString()
                            textBuffer.clear()

                            if (text.contains("\${")) {
                                val macroRegex = Regex("\\$\\{(.+?)}")
                                var lastIndex = 0
                                macroRegex.findAll(text).forEach {
                                    val startOffset = it.range.first
                                    if (startOffset > lastIndex) {
                                        textSpanElements.add(Text(text.substring(lastIndex, startOffset)))
                                    }

                                    textSpanElements.addAll(processInlineMacro(it.groupValues[1], parentNode))

                                    lastIndex = it.range.last + 1
                                }

                                if (lastIndex < text.length) {
                                    textSpanElements.add(Text(text.substring(lastIndex)))
                                }
                            } else {
                                textSpanElements.add(Text(text))
                            }
                        }
                    }

                    do {
                        val consumedNode = consumeNode()
                        when (val consumedNodeType = consumedNode.type) {

                            MarkdownTokenTypes.TEXT -> {
                                consumedNode.getNodeSourceText().also {
                                    if (it.isNotBlank()) {
                                        textBuffer.append(it.trim())
                                    }
                                }
                            }

                            MarkdownTokenTypes.BACKTICK,
                            MarkdownTokenTypes.ESCAPED_BACKTICKS,
                            MarkdownTokenTypes.SINGLE_QUOTE,
                            MarkdownTokenTypes.DOUBLE_QUOTE,
                            MarkdownTokenTypes.LPAREN,
                            MarkdownTokenTypes.RPAREN,
                            MarkdownTokenTypes.LBRACKET,
                            MarkdownTokenTypes.RBRACKET,
                            MarkdownTokenTypes.LT,
                            MarkdownTokenTypes.GT,
                            MarkdownTokenTypes.COLON,
                            MarkdownTokenTypes.EXCLAMATION_MARK ->
                                textBuffer.append(consumedNode.getNodeSourceText())

                            MarkdownTokenTypes.BLOCK_QUOTE -> {
                                if (nextNode?.type == MarkdownTokenTypes.WHITE_SPACE) {
                                    consumeNode()
                                }
                            }

                            MarkdownTokenTypes.EOL,
                            MarkdownTokenTypes.WHITE_SPACE -> {
                                consumeWhiteSpaceNodes()

                                val alreadyHasContent = textSpanElements.isNotEmpty() || textBuffer.isNotEmpty()
                                if (nextNode != null && (isRecursiveCall || alreadyHasContent)) {
                                    textBuffer.append(' ')
                                }
                            }

                            else -> {
                                consumeTextBuffer()

                                when (consumedNodeType) {

                                    MarkdownTokenTypes.HTML_TAG -> {
                                        textSpanElements.add(HtmlTag(consumedNode.getNodeSourceText()))
                                    }

                                    MarkdownTokenTypes.HARD_LINE_BREAK -> {
                                        textSpanElements.add(HardLineBreak())
                                        consumeWhiteSpaceNodes()
                                    }

                                    MarkdownElementTypes.CODE_SPAN -> {
                                        consumedNode
                                            .children
                                            .filter { it.type != MarkdownTokenTypes.BACKTICK }
                                            .joinToString("") { it.getNodeSourceText() }
                                            .also {
                                                if (it.isNotBlank()) {
                                                    textSpanElements.add(InlineCode(it))
                                                }
                                            }
                                    }

                                    GFMTokenTypes.GFM_AUTOLINK -> {
                                        consumedNode
                                            .getNodeSourceText()
                                            .also {
                                                if (it.isNotBlank()) {
                                                    textSpanElements.add(InlineLink(it))
                                                }
                                            }
                                    }

                                    MarkdownElementTypes.IMAGE -> {
                                        consumedNode
                                            .findChildOfType(MarkdownElementTypes.INLINE_LINK)
                                            ?.extractInlineLinkData()?.also {
                                                textSpanElements.add(
                                                    Image(it.linkDestination, it.inlineElements)
                                                )
                                            }
                                    }

                                    MarkdownElementTypes.FULL_REFERENCE_LINK, // TODO ez nem block?
                                    MarkdownElementTypes.SHORT_REFERENCE_LINK, // TODO ez nem block?
                                    MarkdownElementTypes.INLINE_LINK -> {
                                        consumedNode.extractInlineLinkData()?.also {
                                            textSpanElements.add(
                                                InlineLink(it.linkDestination, it.inlineElements)
                                            )
                                        }
                                    }

                                    MarkdownElementTypes.EMPH -> {
                                        consumedNode
                                            .children
                                            .filter { it.type != MarkdownTokenTypes.EMPH }
                                            .convertInlineContent(consumedNode, true)
                                            .also {
                                                if (it.isNotEmpty()) {
                                                    textSpanElements.add(
                                                        TextSpan(Style(italic = ItalicStyle.Italic), it)
                                                    )
                                                }
                                            }
                                    }

                                    MarkdownElementTypes.STRONG -> {
                                        consumedNode
                                            .children
                                            .filter { it.type != MarkdownTokenTypes.EMPH }
                                            .convertInlineContent(consumedNode, true)
                                            .also {
                                                if (it.isNotEmpty()) {
                                                    textSpanElements.add(
                                                        TextSpan(Style(bold = BoldStyle.Bold), it)
                                                    )
                                                }
                                            }
                                    }

                                    else -> {
                                        textSpanElements.add(consumedNode.createErroneousInlineElementReplacement())
                                    }
                                }
                            }
                        }
                    } while (nextNode != null)

                    consumeTextBuffer()

                    return textSpanElements
                }

                // No loop is needed because all sibling AST nodes should be inline
                if (nextNode!!.isInlineNode) {
                    convertTextSpan().forEach {
                        addToResultIfNotNull(it)
                    }
                } else {
                    addToResultIfNotNull(consumeNode().createErroneousInlineElementReplacement())
                }

            }
        } else {
            emptyList()
        }
}

internal fun CharSequence.processMarkdownEscapes(): CharSequence =
    if (!contains('\\')) {
        this
    } else {
        val builder = StringBuilder(length)

        var i = 0
        fun consumeChar() = this[i++]

        val lastIndex = lastIndex
        while (i <= lastIndex) {
            when (val c = consumeChar()) {
                '\\' -> {
                    if (i < lastIndex) {
                        builder.append(consumeChar())
                    }
                }

                else -> {
                    builder.append(c)
                }
            }
        }

        builder
    }
