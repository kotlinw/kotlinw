package kotlinw.util.stdlib

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline infix fun <T, V> T.runCatchingExceptions(block: T.() -> V): Result<V, Exception> {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return try {
        Ok(block())
    } catch (e: Exception) {
        Err(e)
    }
}

fun <V, E> V?.nonNullToOk(otherwise: () -> E): Result<V, E> =
    if (this != null) Ok(this) else Err(otherwise())
