package kotlinw.collection

import kotlinx.collections.immutable.ImmutableSet

class SimpleImmutableSet<E>
private constructor(private val set: Set<E>) : Set<E> by set, ImmutableSet<E> {
    companion object {
        fun <E> Set<E>.toOrderedImmutableSet(): ImmutableSet<E> = SimpleImmutableSet(LinkedHashSet(this))

        fun <E> Set<E>.toImmutableSet(): ImmutableSet<E> =
            if (this is ImmutableSet<E>) this else SimpleImmutableSet(HashSet(this))
    }
}
