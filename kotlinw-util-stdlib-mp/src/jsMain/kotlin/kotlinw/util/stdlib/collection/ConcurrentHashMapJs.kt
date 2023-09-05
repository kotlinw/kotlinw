package kotlinw.util.stdlib.collection

import kotlinw.collection.ConcurrentHashMapInternalImpl

actual class ConcurrentHashMap<K: Any, V: Any>
private constructor(wrapped: ConcurrentHashMapInternalImpl<K, V>) :
    ConcurrentMutableMap<K, V> by wrapped {

    actual constructor() : this(ConcurrentHashMapInternalImpl())

    actual constructor(map: Map<K, V>) : this(ConcurrentHashMapInternalImpl(map))
}
