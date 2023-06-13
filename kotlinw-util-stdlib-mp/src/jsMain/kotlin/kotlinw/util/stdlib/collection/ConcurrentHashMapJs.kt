package kotlinw.util.stdlib.collection

import kotlinw.collection.ConcurrentHashMapInternalImpl

actual class ConcurrentHashMap<K, V>
private constructor(wrapped: ConcurrentHashMapInternalImpl<K, V>) :
    ConcurrentMutableMap<K, V> by wrapped {

    actual constructor() : this(ConcurrentHashMapInternalImpl())

    actual constructor(map: Map<K, V>) : this(ConcurrentHashMapInternalImpl(map))
}
