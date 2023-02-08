package kotlinw.util.stdlib

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun <V, E> Iterable<Result<V, E>>.combineWithAllErrors(): Result<List<V>, List<E>> =
    if (any { it is Err<*> }) {
        Err(filterIsInstance<Err<E>>().map { it.error })
    } else {
        Ok(map { (it as Ok<V>).value })
    }

fun <V, E> combineWithAllErrors(vararg results: Result<V, E>): Result<List<V>, List<E>> =
    results.asIterable().combineWithAllErrors()

fun <V, E, U> produceIfAllOk(vararg results: Result<V, E>, produce: () -> U): Result<U, List<E>> =
    results.asIterable().combineWithAllErrors().map { produce() }

inline fun <V, E> Result<V, E>.recoverFromError(
    block: (E) -> Result<V, E>
): Result<V, E> =
    when (this) {
        is Ok -> this
        is Err -> block(error)
    }

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
