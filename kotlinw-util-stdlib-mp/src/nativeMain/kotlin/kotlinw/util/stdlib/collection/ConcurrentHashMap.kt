package kotlinw.util.stdlib.collection

import kotlinw.collection.ConcurrentHashMapInternalImpl
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

actual class ConcurrentHashMap<K: Any, V: Any>
private constructor(private val wrapped: ConcurrentHashMapInternalImpl<K, V>) : ConcurrentMutableMap<K, V> {

    private val lock = SynchronizedObject()

    actual constructor() : this(ConcurrentHashMapInternalImpl())

    actual constructor(map: Map<K, V>) : this(ConcurrentHashMapInternalImpl(map))

    override fun remove(key: K): V? =
        synchronized(lock) {
            wrapped.remove(key)
        }

    override fun getOrDefault(key: K, defaultValue: V): V =
        synchronized(lock) {
            wrapped.getOrDefault(key, defaultValue)
        }

    override fun putIfAbsent(key: K, value: V): V? =
        synchronized(lock) {
            wrapped.putIfAbsent(key, value)
        }

    override fun remove(key: K, value: V): Boolean =
        synchronized(lock) {
            wrapped.remove(key, value)
        }

    override fun replace(key: K, oldValue: V, newValue: V): Boolean =
        synchronized(lock) {
            wrapped.replace(key, oldValue, newValue)
        }

    override fun replace(key: K, value: V): V? =
        synchronized(lock) {
            wrapped.replace(key, value)
        }

    override fun replaceAll(function: (K, V) -> V) =
        synchronized(lock) {
            wrapped.replaceAll(function)
        }

    override fun computeIfAbsent(key: K, mappingFunction: (K) -> V?): V? =
        synchronized(lock) {
            wrapped.computeIfAbsent(key, mappingFunction)
        }

    override fun computeIfPresent(key: K, remappingFunction: (K, V) -> V?): V? =
        synchronized(lock) {
            wrapped.computeIfPresent(key, remappingFunction)
        }

    override fun compute(key: K, remappingFunction: (K, V?) -> V?): V? =
        synchronized(lock) {
            wrapped.compute(key, remappingFunction)
        }

    override fun merge(key: K, value: V, remappingFunction: (V, V) -> V): V? =
        synchronized(lock) {
            wrapped.merge(key, value, remappingFunction)
        }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() =
            synchronized(lock) {
                wrapped.entries
            }

    override val keys: MutableSet<K>
        get() =
            synchronized(lock) {
                wrapped.keys
            }

    override val size: Int
        get() =
            synchronized(lock) {
                wrapped.size
            }

    override val values: MutableCollection<V>
        get() =
            synchronized(lock) {
                wrapped.values
            }

    override fun clear() {
        synchronized(lock) {
            wrapped.clear()
        }
    }

    override fun isEmpty(): Boolean =
        synchronized(lock) {
            wrapped.isEmpty()
        }

    override fun putAll(from: Map<out K, V>) =
        synchronized(lock) {
            wrapped.putAll(from)
        }

    override fun put(key: K, value: V): V? =
        synchronized(lock) {
            wrapped.put(key, value)
        }

    override fun get(key: K): V? =
        synchronized(lock) {
            wrapped.get(key)
        }

    override fun containsValue(value: V): Boolean =
        synchronized(lock) {
            wrapped.containsValue(value)
        }

    override fun containsKey(key: K): Boolean =
        synchronized(lock) {
            wrapped.containsKey(key)
        }
}
