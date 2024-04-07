package kotlinw.util.stdlib.collection

interface ConcurrentMutableMap<K: Any, V: Any> : MutableMap<K, V> {

    fun getOrDefault(key: K, defaultValue: V): V

    fun putIfAbsent(key: K, value: V): V?

    fun remove(key: K, value: V): Boolean

    fun replace(key: K, oldValue: V, newValue: V): Boolean

    fun replace(key: K, value: V): V?

    fun replaceAll(function: (K, V) -> V)

    fun computeIfAbsent(
        key: K,
        mappingFunction: (K) -> V?
    ): V?

    fun computeIfPresent(
        key: K,
        remappingFunction: (K, V) -> V?
    ): V?

    fun compute(
        key: K,
        remappingFunction: (K, V?) -> V?
    ): V?

    // TODO check signature
    fun merge(
        key: K, value: V,
        remappingFunction: (V, V) -> V
    ): V?
}
