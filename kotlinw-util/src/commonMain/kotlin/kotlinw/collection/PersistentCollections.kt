package kotlinw.collection

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf

fun <E> emptyImmutableList(): ImmutableList<E> = ImmutableArrayList.empty()

fun <E> emptyImmutableOrderedSet(): ImmutableSet<E> = persistentSetOf()

fun <E> emptyImmutableHashSet(): ImmutableSet<E> = persistentHashSetOf()

fun <K, V> emptyImmutableOrderedMap(): ImmutableMap<K, V> = persistentMapOf()

fun <K, V> emptyImmutableHashMap(): ImmutableMap<K, V> = persistentHashMapOf()
