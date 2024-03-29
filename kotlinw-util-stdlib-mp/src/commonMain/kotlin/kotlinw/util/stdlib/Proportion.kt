package kotlinw.util.stdlib

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

@JvmInline
@Serializable
// TODO @Immutable
value class DoubleProportion(val value: Double) {

    companion object {

        val range = 0.0..1.0
    }

    init {
        require(value in range)
    }
}

@JvmInline
@Serializable
// TODO @Immutable
value class FloatProportion(val value: Float) {

    companion object {

        val range = 0.0F..1.0F
    }

    init {
        require(value in range)
    }
}

fun DoubleProportion.toFloatProportion() = FloatProportion(value.toFloat())

fun FloatProportion.toDoubleProportion() = DoubleProportion(value.toDouble())

fun DoubleProportion.toPercentValue(decimalPlaces: Int = 0) = (value * 100.0).round(decimalPlaces)

fun FloatProportion.toPercentValue(decimalPlaces: Int = 0) = (value * 100.0F).round(decimalPlaces)
