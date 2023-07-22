package xyz.kotlinw.objectstore.api

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.serializer
import xyz.kotlinw.keyvaluestore.api.Key

interface ObjectStore {

    suspend fun <T : Any> getOrNull(key: Key, deserializationStrategy: DeserializationStrategy<T>): T?

    suspend fun contains(key: Key): Boolean = getOrNull<Any>(key) != null
}

suspend inline fun <reified T : Any> ObjectStore.getOrNull(key: Key): T? = getOrNull(key, serializer<T>())

suspend fun <T : Any> ObjectStore.get(key: Key, deserializationStrategy: DeserializationStrategy<T>): T =
    getOrNull(key, deserializationStrategy) ?: throw NoSuchElementException()

suspend inline fun <reified T : Any> ObjectStore.get(key: Key): T = get(key, serializer<T>())
