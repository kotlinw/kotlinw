package xyz.kotlinw.objectstore.platform

import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.StringFormat
import xyz.kotlinw.keyvaluestore.platform.PlatformMutableKeyValueStore
import xyz.kotlinw.objectstore.api.MutableObjectStore
import xyz.kotlinw.objectstore.core.DelegatingMutableObjectStore

fun PlatformMutableObjectStore(storeId: String, stringFormat: StringFormat): MutableObjectStore =
    DelegatingMutableObjectStore(PlatformMutableKeyValueStore(storeId), stringFormat)

fun PlatformMutableObjectStore(storeId: String, binaryFormat: BinaryFormat): MutableObjectStore =
    DelegatingMutableObjectStore(PlatformMutableKeyValueStore(storeId), binaryFormat)
