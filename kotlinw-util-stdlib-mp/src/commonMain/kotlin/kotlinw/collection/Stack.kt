package kotlinw.collection

interface Stack<out E: Any>: Collection<E> {

    fun peekOrNull(): E?
}
