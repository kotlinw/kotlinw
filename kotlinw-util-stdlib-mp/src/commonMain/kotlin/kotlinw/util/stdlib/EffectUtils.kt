package kotlinw.util.stdlib

import arrow.core.identity
import arrow.core.raise.EagerEffect
import arrow.core.raise.Effect
import arrow.core.raise.Raise
import arrow.core.raise.RaiseDSL
import arrow.core.raise.fold

suspend inline fun <E, A> Effect<E, A>.getOrElse(crossinline onRaise: (E) -> A): A = fold( { onRaise(it) }, ::identity)

inline fun <E, A> EagerEffect<E, A>.getOrElse(onRaise: (E) -> A): A = fold( { onRaise(it) }, ::identity)
