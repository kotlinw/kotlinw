package xyz.kotlinw.keyvaluestore.api

import kotlinx.io.bytestring.ByteString

interface MutableKeyValueStore : KeyValueStore {

    suspend fun remove(key: Key)

    interface TextValueSupport : MutableKeyValueStore, KeyValueStore.TextValueSupport {

        suspend fun setValue(key: Key, value: String)
    }

    interface BinaryValueSupport : MutableKeyValueStore, KeyValueStore.BinaryValueSupport {

        suspend fun setValue(key: Key, value: ByteString)
    }
}
