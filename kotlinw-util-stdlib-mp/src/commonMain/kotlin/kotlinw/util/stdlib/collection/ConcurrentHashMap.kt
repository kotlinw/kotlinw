package kotlinw.util.stdlib.collection

expect class ConcurrentHashMap<K: Any, V: Any>() : ConcurrentMutableMap<K, V> {
    constructor(map: Map<K, V>)
}
