package xyz.kotlinw.util.unit

import kotlin.jvm.JvmInline

sealed interface BitPerSecondValue<out T: Number> {

    val value: T
}

@JvmInline
internal value class IntBitPerSecondValue(override val value: Int): BitPerSecondValue<Int>

val Int.bps: BitPerSecondValue<Int> get() = IntBitPerSecondValue(this)
