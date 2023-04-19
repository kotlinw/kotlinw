package kotlinw.collection

class ArrayQueue<E : Any> : AbstractMutableCollection<E>(), MutableQueue<E> {

    private val list = ArrayList<E?>()

    private var headIndex = -1

    private inline val tailIndex get() = list.lastIndex

    override fun enqueue(element: E) {
        list.add(element)

        if (headIndex == -1) {
            headIndex = 0
        }
    }

    override fun dequeueOrNull(): E? =
        if (headIndex == -1) {
            null
        } else {
            val element = list[headIndex]
            list[headIndex] = null

            if (headIndex == tailIndex) {
                clear()
            } else {
                headIndex++
            }

            element
        }

    override val size: Int get() = if (headIndex == -1) 0 else tailIndex - headIndex + 1

    override fun add(element: E): Boolean {
        enqueue(element)
        return true
    }

    override fun clear() {
        list.clear()
        headIndex = -1
    }

    override fun iterator(): MutableIterator<E> =
        object : MutableIterator<E> {
            var currentIndex = -1

            var nextIndex = headIndex

            override fun hasNext(): Boolean = if (nextIndex == -1) false else nextIndex <= tailIndex

            override fun next(): E {
                if (!hasNext()) throw NoSuchElementException()

                val element = list[nextIndex]!!
                currentIndex = nextIndex
                nextIndex++
                return element
            }

            override fun remove() {
                check(currentIndex >= 0)

                if (currentIndex == headIndex) {
                    if (headIndex == tailIndex) {
                        clear()
                        // TODO kell? nextIndex = -1
                    } else {
                        list[headIndex] = null
                        headIndex++
                    }
                } else {
                    list.removeAt(currentIndex)
                    nextIndex--
                }

                currentIndex = -1
            }
        }

    override fun peekOrNull(): E? =
        if (headIndex == -1) null else list[headIndex]
}
