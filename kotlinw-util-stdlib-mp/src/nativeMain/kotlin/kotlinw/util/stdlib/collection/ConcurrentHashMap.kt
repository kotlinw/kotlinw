package kotlinw.util.stdlib.collection

actual class ConcurrentHashMap<K, V> actual constructor() : ConcurrentMutableMap<K, V> {

    actual constructor(map: Map<K, V>): this() {
        TODO("Not yet implemented")
    }

    override fun remove(key: K): V? {
        TODO("Not yet implemented")
    }

    override fun getOrDefault(key: K, defaultValue: V): V {
        TODO("Not yet implemented")
    }

    override fun putIfAbsent(key: K, value: V): V? {
        TODO("Not yet implemented")
    }

    override fun remove(key: K, value: V): Boolean {
        TODO("Not yet implemented")
    }

    override fun replace(key: K, oldValue: V, newValue: V): Boolean {
        TODO("Not yet implemented")
    }

    override fun replace(key: K, value: V): V? {
        TODO("Not yet implemented")
    }

    override fun replaceAll(function: (K, V) -> V) {
        TODO("Not yet implemented")
    }

    override fun computeIfAbsent(key: K, mappingFunction: (K) -> V?): V? {
        TODO("Not yet implemented")
    }

    override fun computeIfPresent(key: K, remappingFunction: (K, V) -> V?): V? {
        TODO("Not yet implemented")
    }

    override fun compute(key: K, remappingFunction: (K, V?) -> V?): V? {
        TODO("Not yet implemented")
    }

    override fun merge(key: K, value: V, remappingFunction: (V, V) -> V): V? {
        TODO("Not yet implemented")
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = TODO("Not yet implemented")
    override val keys: MutableSet<K>
        get() = TODO("Not yet implemented")
    override val size: Int
        get() = TODO("Not yet implemented")
    override val values: MutableCollection<V>
        get() = TODO("Not yet implemented")

    override fun clear() {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun putAll(from: Map<out K, V>) {
        TODO("Not yet implemented")
    }

    override fun put(key: K, value: V): V? {
        TODO("Not yet implemented")
    }

    override fun get(key: K): V? {
        TODO("Not yet implemented")
    }

    override fun containsValue(value: V): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsKey(key: K): Boolean {
        TODO("Not yet implemented")
    }
}
