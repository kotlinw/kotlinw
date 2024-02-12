package xyz.kotlinw.util.stdlib

import arrow.core.NonFatal
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract

inline fun runCatchingCleanup(block: () -> Unit) {
    contract {
        callsInPlace(block, EXACTLY_ONCE)
    }

    try {
        block()
    } catch (e: Throwable) {
        if (!NonFatal(e) && e !is kotlin.coroutines.cancellation.CancellationException) {
            throw e
        }
    }
}
