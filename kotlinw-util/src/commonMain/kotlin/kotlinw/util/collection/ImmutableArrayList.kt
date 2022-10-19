package kotlinw.util.collection

import kotlinx.collections.immutable.ImmutableList

sealed interface ImmutableArrayList<E> : ImmutableList<E> {
    companion object {
        fun <E> of(vararg elements: E): ImmutableList<E> =
            if (elements.isEmpty()) {
                empty()
            } else {
                NonEmptyImmutableArrayList(elements)
            }

        @Suppress("UNCHECKED_CAST")
        fun <E> empty(): ImmutableList<E> = EmptyImmutableArrayList.instance as ImmutableList<E>
    }
}

private class EmptyImmutableArrayList<E> : ImmutableArrayList<E> {
    companion object {
        val instance = EmptyImmutableArrayList<Any?>()
    }

    override val size: Int get() = 0

    override fun get(index: Int): E = throw NoSuchElementException()

    override fun isEmpty(): Boolean = true

    override fun iterator(): Iterator<E> = listIterator()

    override fun listIterator(): ListIterator<E> = listIterator(0)

    override fun listIterator(index: Int): ListIterator<E> =
        object : ListIterator<E> {
            override fun hasNext(): Boolean = false

            override fun hasPrevious(): Boolean = false

            override fun next(): E = throw NoSuchElementException()

            override fun nextIndex(): Int = 0

            override fun previous(): E = throw NoSuchElementException()

            override fun previousIndex(): Int = -1
        }

    override fun lastIndexOf(element: E): Int = -1

    override fun indexOf(element: E): Int = -1

    override fun containsAll(elements: Collection<E>): Boolean = false

    override fun contains(element: E): Boolean = false
}

private class NonEmptyImmutableArrayList<E>(private val array: Array<E>) : ImmutableArrayList<E> {
    override val size: Int get() = array.size

    override fun get(index: Int): E = array[index]

    override fun isEmpty(): Boolean = array.isEmpty()

    override fun iterator(): Iterator<E> = array.iterator()

    override fun listIterator(): ListIterator<E> = listIterator(0)

    override fun listIterator(index: Int): ListIterator<E> =
        object : ListIterator<E> {
            private var currentIndex = index

            private fun checkHasNext() {
                if (!hasNext()) {
                    throw NoSuchElementException()
                }
            }

            private fun checkHasPrevious() {
                if (!hasPrevious()) {
                    throw NoSuchElementException()
                }
            }

            override fun hasNext(): Boolean = currentIndex < size

            override fun hasPrevious(): Boolean = currentIndex > 0

            override fun next(): E {
                checkHasNext()
                return array[currentIndex++]
            }

            override fun nextIndex(): Int = currentIndex

            override fun previous(): E {
                checkHasPrevious()
                return array[currentIndex--]
            }

            override fun previousIndex(): Int = currentIndex - 1
        }

    override fun lastIndexOf(element: E): Int = array.lastIndexOf(element)

    override fun indexOf(element: E): Int = array.indexOf(element)

    override fun containsAll(elements: Collection<E>): Boolean = elements.all { array.contains(it) }

    override fun contains(element: E): Boolean = array.contains(element)
}
