package kotlinw.util.collection

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentList.Builder
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

interface Stack<T> : List<T> {
    fun peekOrNull(): T?
}

fun <T> Stack<T>.peek() = peekOrNull() ?: throw NoSuchElementException()

interface MutableStack<T> : Stack<T>, MutableList<T> {
    fun push(value: T)

    fun pop(): T
}

private class MutableStackImpl<T>(private val list: MutableList<T>) : MutableStack<T>, MutableList<T> by list {
    override fun peekOrNull(): T? = list.lastOrNull()

    override fun push(value: T) {
        list.add(value)
    }

    override fun pop(): T = list.removeLast()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as MutableStackImpl<*>

        if (list != other.list) return false

        return true
    }

    override fun hashCode(): Int {
        return list.hashCode()
    }

    override fun toString() = list.toString()
}

fun <E> mutableStackOf(vararg elementsFromBottomToTop: E): MutableStack<E> =
    MutableStackImpl(mutableListOf(*elementsFromBottomToTop))

interface ImmutableStack<T> : Stack<T>, ImmutableList<T>

// TODO @Immutable
sealed interface PersistentStack<T> : ImmutableStack<T>, PersistentList<T> {
    fun push(value: T): PersistentStack<T>

    fun pop(poppedValueConsumer: (T) -> Unit = {}): PersistentStack<T>

    override fun add(element: T): PersistentStack<T>

    override fun add(index: Int, element: T): PersistentStack<T>

    override fun addAll(index: Int, c: Collection<T>): PersistentStack<T>

    override fun addAll(elements: Collection<T>): PersistentStack<T>

    override fun builder(): Builder<T> // TODO ez is PersistentStack-et build-eljen

    override fun clear(): PersistentStack<T>

    override fun remove(element: T): PersistentStack<T>

    override fun removeAll(predicate: (T) -> Boolean): PersistentStack<T>

    override fun removeAll(elements: Collection<T>): PersistentStack<T>

    override fun removeAt(index: Int): PersistentStack<T>

    override fun retainAll(elements: Collection<T>): PersistentStack<T>

    override fun set(index: Int, element: T): PersistentStack<T>
}

// TODO ennek egy hatékonyabb megvalósítást LinkedList-tel
// TODO lehetne value class, de "Value class cannot implement an interface by delegation if expression is not a parameter"
private class PersistentStackImpl<T>(val list: PersistentList<T>) : PersistentStack<T>,
    PersistentList<T> by list {
    override fun peekOrNull(): T? = lastOrNull()

    override fun push(value: T): PersistentStack<T> = PersistentStackImpl(list.add(value))

    override fun pop(poppedValueConsumer: (T) -> Unit): PersistentStack<T> {
        poppedValueConsumer(list.last())
        return PersistentStackImpl(list.removeAt(lastIndex))
    }

    override fun add(element: T): PersistentStack<T> = PersistentStackImpl(list.add(element))

    override fun add(index: Int, element: T): PersistentStack<T> = PersistentStackImpl(list.add(index, element))

    override fun addAll(index: Int, c: Collection<T>): PersistentStack<T> = PersistentStackImpl(list.addAll(index, c))

    override fun addAll(elements: Collection<T>): PersistentStack<T> = PersistentStackImpl(list.addAll(elements))

    override fun clear(): PersistentStack<T> = PersistentStackImpl(list.clear())

    override fun remove(element: T): PersistentStack<T> = PersistentStackImpl(list.remove(element))

    override fun removeAll(predicate: (T) -> Boolean): PersistentStack<T> =
        PersistentStackImpl(list.removeAll(predicate))

    override fun removeAll(elements: Collection<T>): PersistentStack<T> = PersistentStackImpl(list.removeAll(elements))

    override fun removeAt(index: Int): PersistentStack<T> = PersistentStackImpl(list.removeAt(index))

    override fun retainAll(elements: Collection<T>): PersistentStack<T> = PersistentStackImpl(list.retainAll(elements))

    override fun set(index: Int, element: T): PersistentStack<T> = PersistentStackImpl(list.set(index, element))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as PersistentStackImpl<*>

        if (list != other.list) return false

        return true
    }

    override fun hashCode(): Int {
        return list.hashCode()
    }

    override fun toString(): String {
        return list.toString()
    }
}

fun <E> persistentStackOf(vararg elementsFromBottomToTop: E): PersistentStack<E> =
    PersistentStackImpl(persistentListOf(*elementsFromBottomToTop))

fun <E> List<E>.toImmutableStack(): ImmutableStack<E> = PersistentStackImpl(toPersistentList())

fun <E> Stack<E>.toImmutableStack(): ImmutableStack<E> =
    if (this is ImmutableStack<E>) {
        this
    } else {
        (this as List<E>).toImmutableStack()
    }

fun <T> PersistentStack<T>.mutate(mutator: (MutableStack<T>) -> Unit): PersistentStack<T> =
    PersistentStackImpl(
        (this as PersistentStackImpl<T>).list.mutate {
            val mutableStack = MutableStackImpl(it)
            mutator(mutableStack)
        }
    )
