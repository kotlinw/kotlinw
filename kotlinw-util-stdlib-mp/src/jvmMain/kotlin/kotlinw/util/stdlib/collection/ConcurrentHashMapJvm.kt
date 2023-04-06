package kotlinw.util.stdlib.collection

actual class ConcurrentHashMap<K, V> private constructor(
    private val wrapped: java.util.concurrent.ConcurrentMap<K, V>
) : ConcurrentMutableMap<K, V>, MutableMap<K, V> by wrapped {
    actual constructor() : this(java.util.concurrent.ConcurrentHashMap())

    actual constructor(map: Map<K, V>) : this() {
        putAll(map)
    }

    override fun getOrDefault(key: K, defaultValue: V): V = wrapped.getOrDefault(key, defaultValue)

    override fun putIfAbsent(key: K, value: V): V? = wrapped.putIfAbsent(key, value)

    override fun remove(key: K, value: V): Boolean = wrapped.remove(key, value)

    override fun replace(key: K, oldValue: V, newValue: V): Boolean = wrapped.replace(key, oldValue, newValue)

    override fun replace(key: K, value: V): V? = wrapped.replace(key, value)

    override fun replaceAll(function: (K, V) -> V) = wrapped.replaceAll(function)

    override fun computeIfAbsent(key: K, mappingFunction: (K) -> V?): V? = wrapped.computeIfAbsent(key, mappingFunction)

    override fun computeIfPresent(key: K, remappingFunction: (K, V) -> V?): V? =
        wrapped.computeIfPresent(key, remappingFunction)

    override fun compute(key: K, remappingFunction: (K, V?) -> V?): V? = wrapped.compute(key, remappingFunction)

    override fun merge(key: K, value: V, remappingFunction: (V, V) -> V): V? = wrapped.merge(key, value, remappingFunction)

    override fun equals(other: Any?): Boolean =
        if (other is ConcurrentHashMap<*, *>) {
            wrapped == other.wrapped
        } else {
            false
        }

    override fun hashCode(): Int = wrapped.hashCode()

    override fun toString(): String = wrapped.toString()
}
