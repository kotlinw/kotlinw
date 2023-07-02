package kotlinw.util.stdlib.collection

import java.util.*
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ConcurrentHashMap as JavaConcurrentHashMap

actual class ConcurrentHashSet<V>
private constructor(map: ConcurrentMap<V, Boolean>) : ConcurrentMutableSet<V> {
    private val set = Collections.newSetFromMap(map)

    actual constructor() : this(JavaConcurrentHashMap())

    override fun add(element: V): Boolean = set.add(element)

    @Synchronized
    override fun addAll(elements: Collection<V>): Boolean = set.addAll(elements)

    override fun clear() {
        set.clear()
    }

    override fun iterator(): MutableIterator<V> = set.iterator()

    override fun remove(element: V): Boolean = set.remove(element)

    override fun removeAll(elements: Collection<V>): Boolean = set.removeAll(elements.toSet())

    override fun retainAll(elements: Collection<V>): Boolean = set.retainAll(elements.toSet())

    override val size: Int = set.size

    override fun contains(element: V): Boolean = set.contains(element)

    override fun containsAll(elements: Collection<V>): Boolean = set.containsAll(elements)

    override fun isEmpty(): Boolean = set.isEmpty()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ConcurrentHashSet<*>
        return set == other.set
    }

    override fun hashCode(): Int = set.hashCode()

    override fun toString(): String = set.toString()
}
