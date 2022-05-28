package kotlinw.immutator.internal

import kotlinw.immutator.api.ConfinedMutableList
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList

internal class ConfinedMutableListImpl<ElementMutableType, ElementImmutableType>(
    private val initialList: ImmutableList<ElementImmutableType>,
    val toMutable: ElementImmutableType.() -> ElementMutableType,
    val toImmutable: ElementMutableType.() -> ElementImmutableType
) : ConfinedMutableList<ElementMutableType, ElementImmutableType>,
    MutableObjectImplementor<ConfinedMutableList<ElementMutableType, ElementImmutableType>, ImmutableList<ElementImmutableType>> {

    private var persistentList = initialList.map { it.toMutable() }.toPersistentList()

    private var _isMutated = false

    // TODO avoid runtime type checking by implementing a specific ConfinedMutableListOfImmutableElements which need no _isModified checks for each element
    override val _isModified get() = _isMutated || persistentList.any { it is MutableObjectImplementor<*, *> && it._isModified }

    override fun toImmutable(): ImmutableList<ElementImmutableType> =
        if (_isModified) {
            persistentList.map { it.toImmutable() }.toImmutableList()
        } else {
            initialList
        }

    override val size: Int get() = persistentList.size

    override fun contains(element: ElementMutableType): Boolean = persistentList.contains(element)

    override fun containsAll(elements: Collection<ElementMutableType>): Boolean = persistentList.containsAll(elements)

    override fun get(index: Int): ElementMutableType = persistentList.get(index)

    override fun indexOf(element: ElementMutableType): Int = persistentList.indexOf(element)

    override fun isEmpty(): Boolean = persistentList.isEmpty()

    override fun iterator() = object : MutableIterator<ElementMutableType> {
        private val iterator = persistentList.iterator()

        override fun hasNext(): Boolean = iterator.hasNext()

        override fun next(): ElementMutableType = iterator.next()

        override fun remove() = unsupportedMutationOperation()
    }

    private fun unsupportedMutationOperation(): Nothing =
        throw UnsupportedOperationException("Use mutate() for complex modifications.")

    override fun lastIndexOf(element: ElementMutableType): Int = persistentList.lastIndexOf(element)

    override fun listIterator() = listIterator(0)

    override fun listIterator(index: Int) = object : MutableListIterator<ElementMutableType> {
        private val iterator = persistentList.listIterator(index)

        override fun hasPrevious(): Boolean = iterator.hasPrevious()

        override fun nextIndex(): Int = iterator.nextIndex()

        override fun previous(): ElementMutableType = iterator.previous()

        override fun previousIndex(): Int = iterator.previousIndex()

        override fun add(element: ElementMutableType) = unsupportedMutationOperation()

        override fun hasNext(): Boolean = iterator.hasNext()

        override fun next(): ElementMutableType = iterator.next()

        override fun remove() = unsupportedMutationOperation()

        override fun set(element: ElementMutableType) = unsupportedMutationOperation()
    }

    override fun subList(fromIndex: Int, toIndex: Int) = unsupportedMutationOperation()

    override fun <T> mutate(mutator: (MutableList<ElementMutableType>) -> T): T {
        _isMutated = true // TODO optimalize

        data class MutationResult<T>(val value: T)

        lateinit var result: MutationResult<T>
        persistentList = persistentList.mutate {
            result = MutationResult(mutator(it))
        }

        return result.value
    }

    override fun add(element: ElementMutableType) =
        mutate {
            it.add(element)
        }

    override fun add(index: Int, element: ElementMutableType) =
        mutate {
            it.add(index, element)
        }

    override fun addAll(index: Int, elements: Collection<ElementMutableType>) =
        mutate {
            it.addAll(index, elements)
        }

    override fun addAll(elements: Collection<ElementMutableType>): Boolean =
        mutate {
            it.addAll(elements)
        }

    override fun clear() =
        mutate {
            it.clear()
        }

    override fun remove(element: ElementMutableType) =
        mutate {
            it.remove(element)
        }

    override fun removeAll(elements: Collection<ElementMutableType>) =
        mutate {
            it.removeAll(elements)
        }

    override fun removeAt(index: Int) =
        mutate {
            it.removeAt(index)
        }

    override fun retainAll(elements: Collection<ElementMutableType>) =
        mutate {
            it.retainAll(elements)
        }

    override fun set(index: Int, element: ElementMutableType) =
        mutate {
            it.set(index, element)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ConfinedMutableListImpl<*, *>

        if (persistentList != other.persistentList) return false

        return true
    }

    override fun hashCode(): Int {
        return persistentList.hashCode()
    }

    override fun toString(): String {
        return "ConfinedMutableListImpl(persistentList=$persistentList)"
    }
}
