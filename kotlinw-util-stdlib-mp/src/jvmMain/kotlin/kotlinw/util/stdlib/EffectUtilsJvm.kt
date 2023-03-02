@file:OptIn(ExperimentalTypeInference::class)

package kotlinw.util.stdlib

import arrow.core.raise.Effect
import arrow.core.raise.Raise
import arrow.core.raise.RaiseDSL
import kotlin.experimental.ExperimentalTypeInference

context(Raise<R>)
@RaiseDSL
suspend infix fun <R, E, A> Effect<E, A>.getOrElseMapError(@BuilderInference map: (E) -> R): A =
    getOrElse { raise(map(it)) }

@RaiseDSL
suspend infix fun <T: Throwable, E, A> Effect<E, A>.getOrElseThrow(@BuilderInference throwableProvider: (E) -> T): A =
    getOrElse { throw throwableProvider(it) }
