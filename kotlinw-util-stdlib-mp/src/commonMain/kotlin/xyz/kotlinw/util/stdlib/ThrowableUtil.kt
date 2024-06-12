package xyz.kotlinw.util.stdlib

import arrow.core.NonFatal
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import kotlin.coroutines.cancellation.CancellationException

inline fun runCatchingCleanup(block: () -> Unit) {
    contract {
        callsInPlace(block, EXACTLY_ONCE)
    }

    try {
        block()
    } catch (e: Exception) {
        // Ignore even `CancellationException`s
        // TODO log?
        // TODO suppressed exception hozzáadása?
    }
}
