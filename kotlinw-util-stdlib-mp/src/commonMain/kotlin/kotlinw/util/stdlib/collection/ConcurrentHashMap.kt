package kotlinw.util.stdlib.collection

expect class ConcurrentHashMap<K, V>() : ConcurrentMutableMap<K, V> {
    constructor(map: Map<K, V>)
}
