package kotlinw.util.stdlib.collection

import kotlinw.collection.ConcurrentHashMapInternalImpl

// TODO simplify implementation, no synchronization is needed on JS platform
actual class ConcurrentHashMap<K: Any, V: Any>
private constructor(wrapped: ConcurrentHashMapInternalImpl<K, V>) :
    ConcurrentMutableMap<K, V> by wrapped {

    actual constructor() : this(ConcurrentHashMapInternalImpl())

    actual constructor(map: Map<K, V>) : this(ConcurrentHashMapInternalImpl(map))

    actual constructor(initialCapacity: Int): this(ConcurrentHashMapInternalImpl(initialCapacity))
}
