package xyz.kotlinw.util.unit

import xyz.kotlinw.util.unit.ScaledValue.ScaledValueWithUnit
import xyz.kotlinw.util.unit.ScaledValue.UnitlessScaledValue
import xyz.kotlinw.util.unit.ScaledValue.ValueWithUnit

sealed interface BinaryUnit : CoreUnit {

    data object Bit : BinaryUnit {

        override val symbol = "bit"

        val bit = Bit

        val <N : Number> N.bit
            get() =
                ValueWithUnit(this, Bit)

        val <N : Number> UnitlessScaledValue<N>.bit
            get() =
                ScaledValueWithUnit(value, scale, Bit)
    }

    data object Byte : BinaryUnit {

        override val symbol = "byte"

        val byte = Byte

        val <N : Number> UnitlessScaledValue<N>.byte
            get() =
                ScaledValueWithUnit(value, scale, Byte)
    }
}
