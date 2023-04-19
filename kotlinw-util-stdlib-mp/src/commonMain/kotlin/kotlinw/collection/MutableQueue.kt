package kotlinw.collection

interface MutableQueue<E: Any>: MutableCollection<E>, Queue<E> {

    fun enqueue(element: E)

    fun dequeueOrNull(): E?
}
