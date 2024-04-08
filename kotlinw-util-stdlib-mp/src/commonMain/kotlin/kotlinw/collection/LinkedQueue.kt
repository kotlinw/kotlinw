package kotlinw.collection

class LinkedQueue<E : Any> : MutableQueue<E> {

    internal data class Node<E : Any>(val value: E, var next: Node<E>?)

    private var head: Node<E>? = null

    internal inline val headNode get() = head

    private var tail: Node<E>? = null

    internal inline val tailNode get() = tail

    private var currentSize = 0

    constructor()

    constructor(elements: Collection<E>) {
        addAll(elements)
    }

    override fun enqueue(element: E) {
        val node = Node(element, null)

        if (head == null) {
            head = node
        } else {
            tail!!.next = node
        }

        tail = node

        currentSize++
    }

    override fun dequeueOrNull(): E? {
        val headNode = head ?: return null

        head = headNode.next
        currentSize--
        return headNode.value
    }

    override val size: Int get() = currentSize

    override fun clear() {
        head = null
        tail = null
        currentSize = 0
    }

    override fun addAll(elements: Collection<E>): Boolean {
        elements.forEach {
            enqueue(it)
        }

        return elements.isNotEmpty()
    }

    override fun add(element: E): Boolean {
        enqueue(element)
        return true
    }

    override fun isEmpty(): Boolean = currentSize == 0

    override fun containsAll(elements: Collection<E>): Boolean = elements.all { contains(it) }

    override fun contains(element: E): Boolean {
        for (currentElement in this) {
            if (currentElement == element) {
                return true
            }
        }

        return false
    }

    override fun iterator(): MutableIterator<E> = MutableIteratorImpl()

    inner class MutableIteratorImpl : MutableIterator<E> {

        private var previous: Node<E>? = null

        internal inline val previousNode get() = previous

        private var current: Node<E>? = null

        internal inline val currentNode get() = current

        private var next = head

        internal inline val nextNode get() = next

        override fun hasNext(): Boolean = next != null

        override fun next(): E {
            val node = next ?: throw NoSuchElementException()
            next = node.next

            if (current != null) {
                previous = current
            }

            current = node
            return node.value
        }

        override fun remove() {
            val current = current
            check(current != null) { "next() should be called before calling remove()" }

            val previous = previous
            if (previous != null) {
                previous.next = next
            } else {
                head = next
            }

            this.current = null

            if (next == null) {
                tail = previous
            }

            currentSize--
        }
    }

    override fun peekOrNull(): E? = head?.value

    override fun retainAll(elements: Collection<E>): Boolean =
        removeAll { it !in elements }

    override fun removeAll(elements: Collection<E>): Boolean =
        removeAll { it in elements }

    private fun removeAll(predicate: (E) -> Boolean): Boolean {
        val it = iterator()
        var changed = false
        while (it.hasNext()) {
            if (predicate(it.next())) {
                it.remove()
                changed = true
            }
        }
        return changed
    }

    override fun remove(element: E): Boolean {
        val it = iterator()
        while (it.hasNext()) {
            val currentElement = it.next()
            if (currentElement == element) {
                it.remove()
                return true
            }
        }
        return false
    }
}
