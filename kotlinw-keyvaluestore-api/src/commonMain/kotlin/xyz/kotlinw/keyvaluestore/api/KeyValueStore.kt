package xyz.kotlinw.keyvaluestore.api

import kotlinx.io.bytestring.ByteString

typealias Key = String

interface KeyValueStore {

    suspend fun contains(key: Key): Boolean

    interface TextValueSupport : KeyValueStore {

        suspend fun getTextValueOrNull(key: Key): String?
    }

    interface BinaryValueSupport : KeyValueStore {

        suspend fun getBinaryValueOrNull(key: Key): ByteString?
    }
}

suspend fun KeyValueStore.TextValueSupport.getTextValue(key: Key) =
    getTextValueOrNull(key) ?: throw NoSuchElementException()

suspend fun KeyValueStore.BinaryValueSupport.getBinaryValue(key: Key) =
    getBinaryValueOrNull(key) ?: throw NoSuchElementException()
