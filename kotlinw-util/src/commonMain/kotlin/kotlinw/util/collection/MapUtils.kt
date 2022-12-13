package kotlinw.util.collection

fun <K, V> Iterable<Map<K, V>>.merge(): Map<K, V> =
    mutableMapOf<K, V>().also { result -> forEach { result.putAll(it) } }
