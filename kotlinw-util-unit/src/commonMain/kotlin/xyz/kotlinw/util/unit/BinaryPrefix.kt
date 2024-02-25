package xyz.kotlinw.util.unit

import kotlin.math.pow
import xyz.kotlinw.util.unit.BinaryMeasurementFactor.BinaryMeasurementFactorInt
import xyz.kotlinw.util.unit.BinaryPrefixLongName.Exbi
import xyz.kotlinw.util.unit.BinaryPrefixLongName.Gibi
import xyz.kotlinw.util.unit.BinaryPrefixLongName.Kibi
import xyz.kotlinw.util.unit.BinaryPrefixLongName.Mebi
import xyz.kotlinw.util.unit.BinaryPrefixLongName.Pebi
import xyz.kotlinw.util.unit.BinaryPrefixLongName.Tebi
import xyz.kotlinw.util.unit.BinaryPrefixLongName.Yobi
import xyz.kotlinw.util.unit.BinaryPrefixLongName.Zebi
import xyz.kotlinw.util.unit.ScaledValue.ScaledValueWithUnit
import xyz.kotlinw.util.unit.ScaledValue.UnitlessScaledValue

sealed interface BinaryPrefixName {

    val degree: Int

    val symbol: String
}

enum class BinaryPrefixLongName(override val degree: Int) : BinaryPrefixName {
    Kibi(10),
    Mebi(20),
    Gibi(30),
    Tebi(40),
    Pebi(50),
    Exbi(60),
    Zebi(70),
    Yobi(80);

    override val symbol: String get() = name
}

enum class BinaryPrefixShortName(val effectivePrefix: BinaryPrefixLongName) : BinaryPrefixName {
    Ki(Kibi),
    Mi(Mebi),
    Gi(Gibi),
    Ti(Tebi),
    Pi(Pebi),
    Ei(Exbi),
    Zi(Zebi),
    Yi(Yobi);

    override val degree: Int get() = effectivePrefix.degree

    override val symbol: String get() = name
}

internal sealed class BinaryMeasurementFactor<N : Number>(val prefix: BinaryPrefixName) : MeasurementFactor<N> {

    override val symbol: String? get() = prefix.symbol

    internal class BinaryMeasurementFactorInt(prefix: BinaryPrefixName) : BinaryMeasurementFactor<Int>(prefix) {

        override fun apply(value: Int): Int = value * 2.0.pow(prefix.degree).toInt()
    }

    internal class BinaryMeasurementFactorLong(prefix: BinaryPrefixName) : BinaryMeasurementFactor<Long>(prefix) {

        override fun apply(value: Long): Long = value * 2.0.pow(prefix.degree).toLong()
    }

    internal class BinaryMeasurementFactorFloat(prefix: BinaryPrefixName) : BinaryMeasurementFactor<Float>(prefix) {

        override fun apply(value: Float): Float = value * 2F.pow(prefix.degree)
    }

    internal class BinaryMeasurementFactorDouble(prefix: BinaryPrefixName) : BinaryMeasurementFactor<Double>(prefix) {

        override fun apply(value: Double): Double = value * 2.0.pow(prefix.degree)
    }
}

infix fun <U : BinaryUnit> Int.Kibi(unit: U) =
    ScaledValueWithUnit(this, BinaryMeasurementFactorInt(BinaryPrefixLongName.Kibi), unit)

val Int.Kibi
    get() =
        UnitlessScaledValue(this, BinaryMeasurementFactorInt(BinaryPrefixLongName.Kibi))
