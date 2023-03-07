package kotlinw.util.stdlib

import arrow.core.raise.Raise
import arrow.core.raise.RaiseDSL
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

context(Raise<R>)
@OptIn(ExperimentalContracts::class)
@RaiseDSL
public inline fun <R, B : Any> B?.ensureNotNull(raise: () -> R): B {
    contract {
        callsInPlace(raise, InvocationKind.AT_MOST_ONCE)
        returns() implies (this@ensureNotNull != null)
    }
    return this ?: raise(raise())
}
