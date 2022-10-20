package kotlinw.util.collection

import kotlinw.util.collection.ConcurrentMutableSet

actual class ConcurrentHashSet<V> : HashSet<V>(), ConcurrentMutableSet<V>
