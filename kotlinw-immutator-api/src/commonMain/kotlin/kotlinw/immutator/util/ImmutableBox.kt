package kotlinw.immutator.util

import kotlinw.immutator.annotation.Immutable
import kotlinw.uuid.Uuid
import kotlinw.uuid.Uuid.Companion.randomUuid
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlin.reflect.KMutableProperty0

/**
 * Wrapper class to allow mutable types to be referenced by immutable classes.
 * Internal state modification of the referenced mutable object is not tracked or observed.
 */
@Immutable
class ImmutableBox<T>(val value: T) {
    private val uid = randomUuid()

    override fun equals(other: Any?): Boolean = other is ImmutableBox<*> && other.uid == uid

    override fun hashCode(): Int = uid.hashCode()

    override fun toString(): String = value.toString()
}

fun <T> ImmutableBox<T>.mutate(mutator: T.() -> T): ImmutableBox<T> = value.mutator().wrapInImmutableBox()

fun <T> KMutableProperty0<ImmutableBox<T>>.mutate(mutator: T.() -> T) {
    set(get().mutate(mutator))
}

fun <T> T.wrapInImmutableBox() = ImmutableBox(this)

fun <E> ImmutableList<ImmutableBox<E>>.unwrapImmutableBoxElements(): ImmutableList<E> =
    map { it.value }.toImmutableList()

fun <E> List<ImmutableBox<E>>.unwrapImmutableBoxElements(): List<E> = map { it.value }

fun <E> Iterable<E>.toWrappedImmutableList(): ImmutableList<ImmutableBox<E>> =
    map { it.wrapInImmutableBox() }.toImmutableList()
