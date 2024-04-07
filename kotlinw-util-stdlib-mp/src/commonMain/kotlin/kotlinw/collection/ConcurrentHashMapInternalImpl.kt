package kotlinw.collection

import kotlinw.util.stdlib.collection.ConcurrentMutableMap

internal class ConcurrentHashMapInternalImpl<K : Any, V: Any>
private constructor(
    private val wrapped: MutableMap<K, V>,
    @Suppress("UNUSED_PARAMETER") constructorDiscriminator: Unit
) : ConcurrentMutableMap<K, V>, MutableMap<K, V> by wrapped {

    constructor() : this(HashMap(), Unit)

    constructor(map: Map<K, V>) : this() {
        putAll(map)
    }

    constructor(initialCapacity: Int) : this(HashMap(initialCapacity), Unit)

    override fun getOrDefault(key: K, defaultValue: V): V = get(key) ?: defaultValue

    override fun putIfAbsent(key: K, value: V): V? =
        if (!containsKey(key))
            put(key, value)
        else
            get(key)

    override fun remove(key: K, value: V): Boolean =
        if (containsKey(key) && get(key) == value) {
            remove(key)
            true
        } else {
            false
        }

    override fun replace(key: K, oldValue: V, newValue: V): Boolean =
        if (containsKey(key) && get(key) == oldValue
        ) {
            put(key, newValue)
            true
        } else {
            false
        }

    override fun replace(key: K, value: V): V? =
        if (containsKey(key))
            put(key, value)
        else
            null

    override fun replaceAll(function: (K, V) -> V) = TODO()

    override fun computeIfAbsent(key: K, mappingFunction: (K) -> V?): V? {
        val oldValue = get(key)
        return if (oldValue == null) {
            val newValue = mappingFunction(key)
            if (newValue != null) {
                if (putIfAbsent(key, newValue) == null) {
                    newValue
                } else {
                    oldValue
                }
            } else {
                oldValue
            }
        } else {
            oldValue
        }
    }

    override fun computeIfPresent(key: K, remappingFunction: (K, V) -> V?): V? = TODO()

    override fun compute(key: K, remappingFunction: (K, V?) -> V?): V? {
        while (true) {
            val oldValue = get(key)
            val newValue: V? = remappingFunction(key, oldValue)
            if (newValue != null) {
                if (oldValue?.let { replace(key, it, newValue) } ?: (putIfAbsent(
                        key,
                        newValue
                    ) == null)) return newValue
            } else if (oldValue == null || remove(key, oldValue)) {
                return null
            }
        }
    }

    override fun merge(key: K, value: V, remappingFunction: (V, V) -> V): V? {
        while (true) {
            val oldValue = get(key)
            if (oldValue != null) {
                val newValue: V = remappingFunction(oldValue, value)
                if (newValue != null) {
                    if (replace(key, oldValue, newValue)) return newValue
                } else if (remove(key, oldValue)) {
                    return null
                }
            } else if (putIfAbsent(key, value) == null) {
                return value
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConcurrentHashMapInternalImpl<*, *>) return false

        if (wrapped != other.wrapped) return false

        return true
    }

    override fun hashCode(): Int {
        return wrapped.hashCode()
    }

    override fun toString(): String = wrapped.toString()
}
