package xyz.kotlinw.keyvaluestore.platform

import xyz.kotlinw.keyvaluestore.api.MutableKeyValueStore

expect class PlatformMutableKeyValueStore(storeId: String) :
    MutableKeyValueStore.TextValueSupport, MutableKeyValueStore.BinaryValueSupport
