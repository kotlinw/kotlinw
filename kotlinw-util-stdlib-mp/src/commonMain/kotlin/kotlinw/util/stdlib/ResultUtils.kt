package kotlinw.util.stdlib

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
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