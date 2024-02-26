package xyz.kotlinw.util.unit

import kotlin.test.Test
import xyz.kotlinw.util.unit.SiBaseQuantity.Length
import xyz.kotlinw.util.unit.SiBaseQuantity.Time
import xyz.kotlinw.util.unit.SiBaseUnit.meter
import xyz.kotlinw.util.unit.SiBaseUnit.second
import xyz.kotlinw.util.unit.SiUnitFactor.kilo
import xyz.kotlinw.util.unit.ValueWithUnit.DoubleValueWithUnit

class SiUnitTest {

    @Test
    fun testMeter() {
        val a = 1000.0.meter
        val b = 1.0 kilo meter
        val c = 10.0.second
        // val d = a / c
    }
}

typealias Meter<N> = ValueWithUnit<N, meter, Length>

typealias KiloMeter<N> = ValueWithUnit<N, ScaledSiUnit<kilo, meter, Length>, Length>

private fun <
        U : SiUnit<Q>,
        Q : SiQuantity<*, *, *, *, *>
        >
        Double.toDoubleValueWithUnit(unit: U): DoubleValueWithUnit<U, Q> =
    DoubleValueWithUnit(this, unit)

val Double.meter: Meter<Double> get() = toDoubleValueWithUnit(SiBaseUnit.meter)

typealias Second<N> = ValueWithUnit<N, second, Time>

val Double.second: Second<Double> get() = toDoubleValueWithUnit(SiBaseUnit.second)

infix fun <
        U : SiUnit<Q>,
        Q : SiQuantity<*, *, *, *, *>
        >
        Double.kilo(unit: U): ValueWithUnit<Double, ScaledSiUnit<kilo, U, Q>, Q> =
    toDoubleValueWithUnit(unit.scale(kilo))

infix fun Double.kilo(unit: meter): KiloMeter<Double> = kilo<meter, Length>(unit)
