package xyz.kotlinw.util.unit

import kotlin.math.pow
import xyz.kotlinw.util.unit.SiMeasurementFactor.SiMeasurementFactorInt
import xyz.kotlinw.util.unit.ScaledValue.ScaledValueWithUnit
import xyz.kotlinw.util.unit.ScaledValue.UnitlessScaledValue

sealed interface SiPrefixName {

    val degree: Int

    val symbol: String
}

enum class SiPrefixLongName(override val degree: Int) : SiPrefixName {
    yocto(-24),
    zepto(-21),
    atto(-18),
    femto(-15),
    pico(-12),
    nano(-9),
    micro(-6),
    milli(-3),
    centi(-2),
    deci(-1),
    deca(1),
    hecto(2),
    kilo(3),
    mega(6),
    giga(9),
    tera(12),
    peta(15),
    exa(18),
    zetta(21),
    yotta(24);

    override val symbol: String get() = name
}

enum class SiPrefixShortName(val effectivePrefix: SiPrefixLongName) : SiPrefixName {
    y(SiPrefixLongName.yocto),
    z(SiPrefixLongName.zepto),
    a(SiPrefixLongName.atto),
    f(SiPrefixLongName.femto),
    p(SiPrefixLongName.pico),
    n(SiPrefixLongName.nano),
    µ(SiPrefixLongName.micro),
    m(SiPrefixLongName.milli),
    c(SiPrefixLongName.centi),
    d(SiPrefixLongName.deci),
    da(SiPrefixLongName.deca),
    h(SiPrefixLongName.hecto),
    k(SiPrefixLongName.kilo),
    M(SiPrefixLongName.mega),
    G(SiPrefixLongName.giga),
    T(SiPrefixLongName.tera),
    P(SiPrefixLongName.peta),
    E(SiPrefixLongName.exa),
    Z(SiPrefixLongName.zetta),
    Y(SiPrefixLongName.yotta);

    override val degree: Int get() = effectivePrefix.degree

    override val symbol: String get() = name
}

sealed class SiMeasurementFactor<N : Number>(val prefix: SiPrefixName) : MeasurementFactor<N> {

    override val symbol: String? get() = prefix.symbol

    class SiMeasurementFactorInt(prefix: SiPrefixName) : SiMeasurementFactor<Int>(prefix) {

        override fun apply(value: Int): Int = value * 10.0.pow(prefix.degree).toInt()
    }

    class SiMeasurementFactorLong(prefix: SiPrefixName) : SiMeasurementFactor<Long>(prefix) {

        override fun apply(value: Long): Long = value * 10.0.pow(prefix.degree).toLong()
    }

    class SiMeasurementFactorFloat(prefix: SiPrefixName) : SiMeasurementFactor<Float>(prefix) {

        override fun apply(value: Float): Float = value * 10F.pow(prefix.degree)
    }

    class SiMeasurementFactorDouble(prefix: SiPrefixName) : SiMeasurementFactor<Double>(prefix) {

        override fun apply(value: Double): Double = value * 10.0.pow(prefix.degree)
    }
}

infix fun <U : CoreUnit> Int.kilo(unit: U) =
    ScaledValueWithUnit(this, SiMeasurementFactorInt(SiPrefixLongName.kilo), unit)

val Int.kilo
    get() =
        UnitlessScaledValue(this, SiMeasurementFactorInt(SiPrefixLongName.kilo))
