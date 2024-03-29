package kotlinw.util.stdlib.collection

fun <K, V> Iterable<Map<K, V>>.merge(): Map<K, V> =
    mutableMapOf<K, V>().also { result -> forEach { result.putAll(it) } }

fun <K, V> Map<K, V>.containsKeys(keys: Iterable<K>) = keys.all { containsKey(it) }

@Suppress("UNCHECKED_CAST")
fun <K, V> Map<K, V?>.filterNotNullValues(): Map<K, V> = filterValues { it != null } as Map<K, V>
