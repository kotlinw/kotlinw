package kotlinw.collection

interface Queue<out E: Any>: Collection<E> {

    fun peekOrNull(): E?
}
