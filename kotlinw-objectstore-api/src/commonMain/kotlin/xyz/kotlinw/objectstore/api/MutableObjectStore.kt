package xyz.kotlinw.objectstore.api

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.serializer
import xyz.kotlinw.keyvaluestore.api.Key

interface MutableObjectStore: ObjectStore {

    suspend fun <T : Any> set(key: Key, value: T, serializationStrategy: SerializationStrategy<T>)

    suspend fun remove(key: Key)
}

suspend inline fun <reified T : Any> MutableObjectStore.set(key: Key, value: T) {
    set(key, value, serializer<T>())
}
