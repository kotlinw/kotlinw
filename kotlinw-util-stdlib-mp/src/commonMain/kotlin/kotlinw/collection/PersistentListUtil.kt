package kotlinw.collection

import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.update
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate

fun <E> AtomicRef<PersistentList<E>>.mutate(mutator: (MutableList<E>) -> Unit) {
    update {
        it.mutate(mutator)
    }
}
