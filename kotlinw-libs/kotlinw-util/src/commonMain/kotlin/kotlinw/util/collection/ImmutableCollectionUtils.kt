package kotlinw.util.collection

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.adapters.ImmutableMapAdapter
import kotlinx.collections.immutable.adapters.ImmutableSetAdapter
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf

fun <E> emptyImmutableList(): ImmutableList<E> = ImmutableArrayList.empty()

fun <E> emptyImmutableOrderedSet(): ImmutableSet<E> = persistentSetOf()

fun <E> emptyImmutableHashSet(): ImmutableSet<E> = persistentHashSetOf()

fun <K, V> emptyImmutableOrderedMap(): ImmutableMap<K, V> = persistentMapOf()

fun <K, V> emptyImmutableHashMap(): ImmutableMap<K, V> = persistentHashMapOf()

fun <E> Set<E>.toImmutableOrderedSet(): ImmutableSet<E> = LinkedHashSet(this).wrapAsImmutableSet()

fun <E> Set<E>.toImmutableSet(): ImmutableSet<E> =
    if (this is ImmutableSet<E>) this else HashSet(this).wrapAsImmutableSet()

private fun <E> Set<E>.wrapAsImmutableSet(): ImmutableSet<E> = ImmutableSetAdapter(this)

fun <K, V> Map<K, V>.toImmutableOrderedMap(): ImmutableMap<K, V> = LinkedHashMap(this).wrapAsImmutableMap()

fun <K, V> Map<K, V>.toImmutableMap(): ImmutableMap<K, V> =
    if (this is ImmutableMap<K, V>) this else HashMap(this).wrapAsImmutableMap()

private fun <K, V> Map<K, V>.wrapAsImmutableMap(): ImmutableMap<K, V> = ImmutableMapAdapter(this)
