package kotlinw.util.collection

interface ConcurrentMutableSet<V>: MutableSet<V>

expect class ConcurrentHashSet<V>: ConcurrentMutableSet<V>
