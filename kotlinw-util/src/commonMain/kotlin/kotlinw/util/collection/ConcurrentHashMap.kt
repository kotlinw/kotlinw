package kotlinw.util.collection

expect class ConcurrentHashMap<K, V>() : ConcurrentMutableMap<K, V> {
    constructor(map: Map<K, V>)
}
