package kotlinw.util.stdlib

import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList

class NodeListIterator(
    private val nodeList: NodeList
) : Iterator<Node> {

    private var index = 0

    override fun hasNext(): Boolean = index < nodeList.length

    override fun next(): Node {
        if (!hasNext()) {
            throw NoSuchElementException()
        }

        return nodeList.item(index++)
    }
}

fun NodeList.iterator(): Iterator<Node> = NodeListIterator(this)

fun NodeList.toIterable(): Iterable<Node> = Iterable { iterator() }

val Element.childElements: List<Element> get() = childNodes.toIterable().filterIsInstance<Element>()

fun List<Element>.filterByName(name: String): List<Element> = filter { it.tagName == name }

fun Element.childElementsByName(name: String): List<Element> = childElements.filterByName(name)

fun Element.firstChildElementByName(name: String): Element? = childElementsByName(name).firstOrNull()

fun Element.getNormalizedAttribute(name: String): String? = getAttribute(name).ifBlankNull()
