package kotlinw.util.stdlib

import arrow.core.nonFatalOrThrow
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline infix fun <T, V> T.runCatchingExceptions(block: T.() -> V): Result<V, Throwable> {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return try {
        Ok(block())
    } catch (e: Throwable) {
        Err(e.nonFatalOrThrow())
    }
}

fun <V, E> V?.nonNullToOk(otherwise: () -> E): Result<V, E> =
    if (this != null) Ok(this) else Err(otherwise())
