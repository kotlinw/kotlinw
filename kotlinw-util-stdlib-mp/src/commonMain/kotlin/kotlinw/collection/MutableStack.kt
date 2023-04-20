package kotlinw.collection

interface MutableStack<E: Any>: MutableCollection<E>, Stack<E> {

    fun push(element: E)

    fun popOrNull(): E?
}
