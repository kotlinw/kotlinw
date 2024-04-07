package kotlinw.util.stdlib.collection

import kotlinw.collection.MutableQueue
import java.util.concurrent.ConcurrentLinkedQueue

actual class ConcurrentLinkedQueue<E : Any> actual constructor() : MutableQueue<E> {

    private val wrapped: ConcurrentLinkedQueue<E> = ConcurrentLinkedQueue()

    override fun enqueue(element: E) {
        wrapped.add(element)
    }

    override fun dequeueOrNull(): E? = wrapped.poll()

    override val size: Int get() = wrapped.size

    override fun clear() {
        wrapped.clear()
    }

    override fun isEmpty(): Boolean = wrapped.isEmpty()

    override fun iterator(): MutableIterator<E> = wrapped.iterator()

    override fun peekOrNull(): E? = wrapped.peek()

    override fun retainAll(elements: Collection<E>): Boolean = wrapped.retainAll(elements)

    override fun removeAll(elements: Collection<E>): Boolean = wrapped.removeAll(elements)

    override fun remove(element: E): Boolean = wrapped.remove(element)

    override fun containsAll(elements: Collection<E>): Boolean = wrapped.containsAll(elements)

    override fun contains(element: E): Boolean = wrapped.contains(element)

    override fun addAll(elements: Collection<E>): Boolean = wrapped.addAll(elements)

    override fun add(element: E): Boolean = wrapped.add(element)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is kotlinw.util.stdlib.collection.ConcurrentLinkedQueue<*>) return false

        if (wrapped != other.wrapped) return false

        return true
    }

    override fun hashCode(): Int {
        return wrapped.hashCode()
    }

    override fun toString(): String = wrapped.toString()
}
