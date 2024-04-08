package kotlinw.util.stdlib.collection

import kotlinw.collection.MutableQueue

expect class ConcurrentLinkedQueue<E : Any> : MutableQueue<E> {

    constructor()

    constructor(elements: Collection<E>)
}
