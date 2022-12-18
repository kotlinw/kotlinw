package kotlinw.util.stdlib

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun <V, E> Result<V, E>.ifError(block: (E) -> Unit): Result<V, E> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    if (this is Err<*>) {
        block(error as E)
    }

    return this
}

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
