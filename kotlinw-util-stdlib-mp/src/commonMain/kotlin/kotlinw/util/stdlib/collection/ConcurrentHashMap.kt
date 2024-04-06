package kotlinw.util.stdlib.collection

expect class ConcurrentHashMap<K: Any, V>() : ConcurrentMutableMap<K, V> {

    constructor(map: Map<K, V>)

    constructor(initialCapacity: Int)
}
