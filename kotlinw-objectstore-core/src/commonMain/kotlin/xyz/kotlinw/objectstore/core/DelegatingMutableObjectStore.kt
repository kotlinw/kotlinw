package xyz.kotlinw.objectstore.core

import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialFormat
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.StringFormat
import xyz.kotlinw.keyvaluestore.api.Key
import xyz.kotlinw.keyvaluestore.api.KeyValueStore
import xyz.kotlinw.keyvaluestore.api.MutableKeyValueStore
import xyz.kotlinw.objectstore.api.MutableObjectStore

class DelegatingMutableObjectStore
private constructor(
    private val keyValueStore: MutableKeyValueStore,
    private val serialFormat: SerialFormat,
    private val isBinary: Boolean
) : MutableObjectStore {

    constructor(keyValueStore: MutableKeyValueStore.TextValueSupport, serialFormat: StringFormat) :
            this(keyValueStore, serialFormat, false)

    constructor(keyValueStore: MutableKeyValueStore.BinaryValueSupport, serialFormat: BinaryFormat) :
            this(keyValueStore, serialFormat, true)

    override suspend fun <T : Any> getOrNull(key: Key, deserializationStrategy: DeserializationStrategy<T>): T? =
        if (isBinary) {
            (keyValueStore as KeyValueStore.BinaryValueSupport).getBinaryValueOrNull(key)
                ?.let { (serialFormat as BinaryFormat).decodeFromByteArray(deserializationStrategy, it.toByteArray()) }
        } else {
            (keyValueStore as KeyValueStore.TextValueSupport).getTextValueOrNull(key)
                ?.let { (serialFormat as StringFormat).decodeFromString(deserializationStrategy, it) }
        }

    override suspend fun <T : Any> set(key: Key, value: T, serializationStrategy: SerializationStrategy<T>) {
        if (isBinary) {
            (keyValueStore as MutableKeyValueStore.BinaryValueSupport).setValue(
                key,
                ByteString((serialFormat as BinaryFormat).encodeToByteArray(serializationStrategy, value))
            )
        } else {
            (keyValueStore as MutableKeyValueStore.TextValueSupport).setValue(
                key,
                (serialFormat as StringFormat).encodeToString(serializationStrategy, value)
            )
        }
    }

    override suspend fun remove(key: Key) {
        keyValueStore.remove(key)
    }
}
