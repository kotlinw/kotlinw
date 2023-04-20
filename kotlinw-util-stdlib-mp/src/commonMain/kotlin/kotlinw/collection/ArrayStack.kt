package kotlinw.collection

class ArrayStack<E : Any>(initialCapacity: Int = defaultInitialCapacity) :
    AbstractMutableCollection<E>(), MutableStack<E> {

    companion object {

        const val defaultInitialCapacity = 16
    }

    private val list = ArrayList<E>(initialCapacity)

    override fun push(element: E) {
        list.add(element)
    }

    override fun popOrNull(): E? = list.removeLastOrNull()

    override val size: Int get() = list.size

    override fun iterator(): MutableIterator<E> =
        object : MutableIterator<E> {

            var nextIndex = size

            override fun hasNext(): Boolean = nextIndex > 0

            override fun next(): E {
                if (!hasNext()) throw NoSuchElementException()
                return list[--nextIndex]
            }

            override fun remove() {
                check(nextIndex in -1 until size)
                list.removeAt(nextIndex + 1)
            }
        }

    override fun peekOrNull(): E? = lastOrNull()

    override fun add(element: E): Boolean {
        push(element)
        return true
    }
}
