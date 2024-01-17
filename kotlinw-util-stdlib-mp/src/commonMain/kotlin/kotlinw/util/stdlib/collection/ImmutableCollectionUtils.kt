package kotlinw.util.stdlib.collection

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.adapters.ImmutableMapAdapter
import kotlinx.collections.immutable.adapters.ImmutableSetAdapter
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

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

typealias SerializableImmutableList<T> = @Serializable(ImmutableListSerializer::class) ImmutableList<T>

@Serializer(forClass = ImmutableList::class)
class ImmutableListSerializer<T>(private val dataSerializer: KSerializer<T>) : KSerializer<ImmutableList<T>> {

    private class PersistentListDescriptor : SerialDescriptor by serialDescriptor<List<String>>() {

        override val serialName: String = "kotlinx.collections.immutable.ImmutableList"
    }

    override val descriptor: SerialDescriptor = PersistentListDescriptor()

    override fun serialize(encoder: Encoder, value: ImmutableList<T>) =
        ListSerializer(dataSerializer).serialize(encoder, value.toList())

    override fun deserialize(decoder: Decoder): ImmutableList<T> =
        ListSerializer(dataSerializer).deserialize(decoder).toPersistentList()
}

typealias SerializableImmutableSet<T> = @Serializable(ImmutableSetSerializer::class) ImmutableSet<T>

@Serializer(forClass = ImmutableSet::class)
class ImmutableSetSerializer<T>(private val dataSerializer: KSerializer<T>) : KSerializer<ImmutableSet<T>> {

    private class PersistentSetDescriptor : SerialDescriptor by serialDescriptor<Set<String>>() {

        override val serialName: String = "kotlinx.collections.immutable.ImmutableSet"
    }

    override val descriptor: SerialDescriptor = PersistentSetDescriptor()

    override fun serialize(encoder: Encoder, value: ImmutableSet<T>) =
        SetSerializer(dataSerializer).serialize(encoder, value.toSet())

    override fun deserialize(decoder: Decoder): ImmutableSet<T> =
        SetSerializer(dataSerializer).deserialize(decoder).toPersistentSet()
}

typealias SerializableImmutableMap<K, V> = @Serializable(ImmutableMapSerializer::class) ImmutableMap<K, V>

@Serializer(forClass = ImmutableMap::class)
class ImmutableMapSerializer<K, V>(
    private val keySerializer: KSerializer<K>,
    private val valueSerializer: KSerializer<V>
) : KSerializer<ImmutableMap<K, V>> {

    private class PersistentMapDescriptor : SerialDescriptor by serialDescriptor<Map<String, String>>() {

        override val serialName: String = "kotlinx.collections.immutable.ImmutableMap"
    }

    override val descriptor: SerialDescriptor = PersistentMapDescriptor()

    override fun serialize(encoder: Encoder, value: ImmutableMap<K, V>) =
        MapSerializer(keySerializer, valueSerializer).serialize(encoder, value.toMap())

    override fun deserialize(decoder: Decoder): ImmutableMap<K, V> =
        MapSerializer(keySerializer, valueSerializer).deserialize(decoder).toPersistentMap()
}
