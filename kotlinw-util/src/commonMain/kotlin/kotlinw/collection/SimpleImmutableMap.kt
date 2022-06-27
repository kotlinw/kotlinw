package kotlinw.collection

import kotlinx.collections.immutable.ImmutableCollection
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.adapters.ImmutableCollectionAdapter
import kotlinx.collections.immutable.adapters.ImmutableSetAdapter
import kotlinx.serialization.Serializable
import kotlin.collections.Map.Entry

@Serializable
class SimpleImmutableMap<K, V>
private constructor(val map: Map<K, V>) : Map<K, V> by map, ImmutableMap<K, V> {
    companion object {
        fun <K, V> Map<K, V>.toOrderedImmutableMap(): ImmutableMap<K, V> = SimpleImmutableMap(LinkedHashMap(this))

        fun <K, V> Map<K, V>.toImmutableMap(): ImmutableMap<K, V> =
            if (this is ImmutableMap<K, V>) this else SimpleImmutableMap(HashMap(this))
    }

    override val entries: ImmutableSet<Entry<K, V>> get() = ImmutableSetAdapter(map.entries)

    override val keys: ImmutableSet<K> get() = ImmutableSetAdapter(map.keys)

    override val values: ImmutableCollection<V> get() = ImmutableCollectionAdapter(map.values)
}
