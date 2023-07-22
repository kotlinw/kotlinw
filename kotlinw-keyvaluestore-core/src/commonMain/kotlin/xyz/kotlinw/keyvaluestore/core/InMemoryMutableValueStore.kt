package xyz.kotlinw.keyvaluestore.core

import kotlinw.util.stdlib.collection.ConcurrentHashMap
import kotlinw.util.stdlib.collection.ConcurrentMutableMap
import kotlinx.io.bytestring.ByteString
import xyz.kotlinw.keyvaluestore.api.Key
import xyz.kotlinw.keyvaluestore.api.MutableKeyValueStore

class InMemoryMutableValueStore : MutableKeyValueStore, MutableKeyValueStore.TextValueSupport, MutableKeyValueStore.BinaryValueSupport {

    private val map: ConcurrentMutableMap<Key, Any> = ConcurrentHashMap()

    override suspend fun setValue(key: Key, value: String) {
        map[key] = value
    }

    override suspend fun setValue(key: Key, value: ByteString) {
        map[key] = value
    }

    override suspend fun remove(key: Key) {
        map.remove(key)
    }

    override suspend fun contains(key: Key): Boolean = map.contains(key)

    override suspend fun getBinaryValueOrNull(key: Key): ByteString? =
        map[key]?.let {
            if (it is ByteString)
                it
            else
                throw IllegalStateException()
        }

    override suspend fun getTextValueOrNull(key: Key): String? =
        map[key]?.let {
            if (it is String)
                it
            else
                throw IllegalStateException()
        }
}
