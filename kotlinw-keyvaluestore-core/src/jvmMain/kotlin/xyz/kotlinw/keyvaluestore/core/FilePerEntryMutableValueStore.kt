package xyz.kotlinw.keyvaluestore.core

import kotlinx.io.bytestring.ByteString
import xyz.kotlinw.keyvaluestore.api.Key
import xyz.kotlinw.keyvaluestore.api.MutableKeyValueStore

class FilePerEntryMutableValueStore(storeId: String): MutableKeyValueStore.TextValueSupport, MutableKeyValueStore.BinaryValueSupport {

    override suspend fun setValue(key: Key, value: String) {
        TODO("Not yet implemented")
    }

    override suspend fun setValue(key: Key, value: ByteString) {
        TODO("Not yet implemented")
    }

    override suspend fun remove(key: Key) {
        TODO("Not yet implemented")
    }

    override suspend fun contains(key: Key): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun getBinaryValueOrNull(key: Key): ByteString? {
        TODO("Not yet implemented")
    }

    override suspend fun getTextValueOrNull(key: Key): String? {
        TODO("Not yet implemented")
    }
}
