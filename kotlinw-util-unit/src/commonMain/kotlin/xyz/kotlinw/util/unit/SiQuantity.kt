package xyz.kotlinw.util.unit

import kotlin.reflect.KType
import kotlin.reflect.typeOf
import xyz.kotlinw.util.unit.DecimalExponent.Companion.exponentFor
import xyz.kotlinw.util.unit.DecimalExponent.ConstantDecimalExponent.M1
import xyz.kotlinw.util.unit.DecimalExponent.ConstantDecimalExponent.M2
import xyz.kotlinw.util.unit.DecimalExponent.ConstantDecimalExponent.M3
import xyz.kotlinw.util.unit.DecimalExponent.ConstantDecimalExponent.M4
import xyz.kotlinw.util.unit.DecimalExponent.ConstantDecimalExponent.M5
import xyz.kotlinw.util.unit.DecimalExponent.ConstantDecimalExponent.P1
import xyz.kotlinw.util.unit.DecimalExponent.ConstantDecimalExponent.P2
import xyz.kotlinw.util.unit.DecimalExponent.ConstantDecimalExponent.P3
import xyz.kotlinw.util.unit.DecimalExponent.ConstantDecimalExponent.P4
import xyz.kotlinw.util.unit.DecimalExponent.ConstantDecimalExponent.P5
import xyz.kotlinw.util.unit.DecimalExponent.Div
import xyz.kotlinw.util.unit.DecimalExponent.Excluded
import xyz.kotlinw.util.unit.DecimalExponent.Mul

sealed interface DecimalExponent : QuantityExponent {

    companion object {

        private val entries = listOf(
            M5, M4, M3, M2, M1,
            P1, P2, P3, P4, P5
        )

        @PublishedApi
        internal inline fun <reified T : DecimalExponent> exponentFor() =
            typeOf<T>().exponentFor()

        private fun KType.isExcluded(): Boolean =
            when (classifier) {
                Excluded::class -> true

                Mul::class, Div::class -> arguments[0].type!!.isExcluded() && arguments[1].type!!.isExcluded()

                else -> false
            }

        @PublishedApi
        internal fun KType.exponentFor(): Int? =
            if (isExcluded())
                Excluded.exponent
            else
                when (classifier) {
                    Mul::class -> {
                        val argument0KType = arguments[0].type!!
                        val argument1KType = arguments[1].type!!
                        (argument0KType.exponentFor() ?: 0) + (argument1KType.exponentFor() ?: 0)
                    }

                    Div::class -> {
                        val argument0KType = arguments[0].type!!
                        val argument1KType = arguments[1].type!!
                        (argument0KType.exponentFor() ?: 0) - (argument1KType.exponentFor() ?: 0)
                    }

                    else -> entries.first { it::class == classifier }.exponent
                }
    }

    val exponent: Int?

    data object Excluded : DecimalExponent {

        override val exponent: Int? get() = null
    }

    data class Mul<out E1 : DecimalExponent, out E2 : DecimalExponent>(val e1: E1, val e2: E2) : DecimalExponent {

        override val exponent: Int?
            get() {
                val exponent1 = e1.exponent
                val exponent2 = e2.exponent
                return if (exponent1 == null && exponent2 == null)
                    null
                else
                    (exponent1 ?: 0) + (exponent2 ?: 0)
            }
    }

    data class Div<out E1 : DecimalExponent, out E2 : DecimalExponent>(val e1: E1, val e2: E2) : DecimalExponent {

        override val exponent: Int?
            get() {
                val exponent1 = e1.exponent
                val exponent2 = e2.exponent
                return if (exponent1 == null && exponent2 == null)
                    null
                else
                    (exponent1 ?: 0) - (exponent2 ?: 0)
            }
    }

    sealed interface ConstantDecimalExponent : DecimalExponent {

        data object M5 : ConstantDecimalExponent {
            override val exponent: Int get() = -5
        }

        data object M4 : ConstantDecimalExponent {
            override val exponent: Int get() = -4
        }

        data object M3 : ConstantDecimalExponent {
            override val exponent: Int get() = -3
        }

        data object M2 : ConstantDecimalExponent {
            override val exponent: Int get() = -2
        }

        data object M1 : ConstantDecimalExponent {
            override val exponent: Int get() = -1
        }

        data object P1 : ConstantDecimalExponent {
            override val exponent: Int get() = 1
        }

        data object P2 : ConstantDecimalExponent {
            override val exponent: Int get() = 2
        }

        data object P3 : ConstantDecimalExponent {
            override val exponent: Int get() = 3
        }

        data object P4 : ConstantDecimalExponent {
            override val exponent: Int get() = 4
        }

        data object P5 : ConstantDecimalExponent {
            override val exponent: Int get() = 5
        }
    }
}

sealed interface SiQuantity<
        Time : DecimalExponent,
        Length : DecimalExponent,
        Mass : DecimalExponent,
        ElectricCurrent : DecimalExponent,
        ThermodynamicTemperature : DecimalExponent,
        AmountOfSubstance : DecimalExponent,
        LuminousIntensity : DecimalExponent
        >
    : Quantity {

    val timeExponent: Int?
    val lengthExponent: Int?
    val massExponent: Int?
    val electricCurrentExponent: Int?
    val thermodynamicTemperatureExponent: Int?
    val amountOfSubstanceExponent: Int?
    val luminousIntensityExponent: Int?
}

@PublishedApi
internal data class SiQuantityImpl<Time : DecimalExponent, Length : DecimalExponent, Mass : DecimalExponent, ElectricCurrent : DecimalExponent, ThermodynamicTemperature : DecimalExponent, AmountOfSubstance : DecimalExponent, LuminousIntensity : DecimalExponent>(
    override val timeExponent: Int?,
    override val lengthExponent: Int?,
    override val massExponent: Int?,
    override val electricCurrentExponent: Int?,
    override val thermodynamicTemperatureExponent: Int?,
    override val amountOfSubstanceExponent: Int?,
    override val luminousIntensityExponent: Int?
) : SiQuantity<Time, Length, Mass, ElectricCurrent, ThermodynamicTemperature, AmountOfSubstance, LuminousIntensity>

inline fun <
        reified Time : DecimalExponent,
        reified Length : DecimalExponent,
        reified Mass : DecimalExponent,
        reified ElectricCurrent : DecimalExponent,
        reified ThermodynamicTemperature : DecimalExponent,
        reified AmountOfSubstance : DecimalExponent,
        reified LuminousIntensity : DecimalExponent
        > SiQuantity():
        SiQuantity<Time, Length, Mass, ElectricCurrent, ThermodynamicTemperature, AmountOfSubstance, LuminousIntensity> =
    SiQuantityImpl(
        exponentFor<Time>(),
        exponentFor<Length>(),
        exponentFor<Mass>(),
        exponentFor<ElectricCurrent>(),
        exponentFor<ThermodynamicTemperature>(),
        exponentFor<AmountOfSubstance>(),
        exponentFor<LuminousIntensity>()
    )

inline operator fun <
        reified Time1 : DecimalExponent,
        reified Length1 : DecimalExponent,
        reified Mass1 : DecimalExponent,
        reified ElectricCurrent1 : DecimalExponent,
        reified ThermodynamicTemperature1 : DecimalExponent,
        reified AmountOfSubstance1 : DecimalExponent,
        reified LuminousIntensity1 : DecimalExponent,
        reified Time2 : DecimalExponent,
        reified Length2 : DecimalExponent,
        reified Mass2 : DecimalExponent,
        reified ElectricCurrent2 : DecimalExponent,
        reified ThermodynamicTemperature2 : DecimalExponent,
        reified AmountOfSubstance2 : DecimalExponent,
        reified LuminousIntensity2 : DecimalExponent
        >
        SiQuantity<Time1, Length1, Mass1, ElectricCurrent1, ThermodynamicTemperature1, AmountOfSubstance1, LuminousIntensity1>.times(
    other: SiQuantity<Time2, Length2, Mass2, ElectricCurrent2, ThermodynamicTemperature2, AmountOfSubstance2, LuminousIntensity2>
):
        SiQuantity<
                Mul<Time1, Time2>,
                Mul<Length1, Length2>,
                Mul<Mass1, Mass2>,
                Mul<ElectricCurrent1, ElectricCurrent2>,
                Mul<ThermodynamicTemperature1, ThermodynamicTemperature2>,
                Mul<AmountOfSubstance1, AmountOfSubstance2>,
                Mul<LuminousIntensity1, LuminousIntensity2>
                > {
    return SiQuantityImpl(
        exponentFor<Mul<Time1, Time2>>(),
        exponentFor<Mul<Length1, Length2>>(),
        exponentFor<Mul<Mass1, Mass2>>(),
        exponentFor<Mul<ElectricCurrent1, ElectricCurrent2>>(),
        exponentFor<Mul<ThermodynamicTemperature1, ThermodynamicTemperature2>>(),
        exponentFor<Mul<AmountOfSubstance1, AmountOfSubstance2>>(),
        exponentFor<Mul<LuminousIntensity1, LuminousIntensity2>>()
    )
}

inline operator fun <
        reified Time1 : DecimalExponent,
        reified Length1 : DecimalExponent,
        reified Mass1 : DecimalExponent,
        reified ElectricCurrent1 : DecimalExponent,
        reified ThermodynamicTemperature1 : DecimalExponent,
        reified AmountOfSubstance1 : DecimalExponent,
        reified LuminousIntensity1 : DecimalExponent,
        reified Time2 : DecimalExponent,
        reified Length2 : DecimalExponent,
        reified Mass2 : DecimalExponent,
        reified ElectricCurrent2 : DecimalExponent,
        reified ThermodynamicTemperature2 : DecimalExponent,
        reified AmountOfSubstance2 : DecimalExponent,
        reified LuminousIntensity2 : DecimalExponent
        >
        SiQuantity<Time1, Length1, Mass1, ElectricCurrent1, ThermodynamicTemperature1, AmountOfSubstance1, LuminousIntensity1>.div(
    other: SiQuantity<Time2, Length2, Mass2, ElectricCurrent2, ThermodynamicTemperature2, AmountOfSubstance2, LuminousIntensity2>
):
        SiQuantity<
                Div<Time1, Time2>,
                Div<Length1, Length2>,
                Div<Mass1, Mass2>,
                Div<ElectricCurrent1, ElectricCurrent2>,
                Div<ThermodynamicTemperature1, ThermodynamicTemperature2>,
                Div<AmountOfSubstance1, AmountOfSubstance2>,
                Div<LuminousIntensity1, LuminousIntensity2>
                > =
    SiQuantityImpl(
        exponentFor<Div<Time1, Time2>>(),
        exponentFor<Div<Length1, Length2>>(),
        exponentFor<Div<Mass1, Mass2>>(),
        exponentFor<Div<ElectricCurrent1, ElectricCurrent2>>(),
        exponentFor<Div<ThermodynamicTemperature1, ThermodynamicTemperature2>>(),
        exponentFor<Div<AmountOfSubstance1, AmountOfSubstance2>>(),
        exponentFor<Div<LuminousIntensity1, LuminousIntensity2>>()
    )

@PublishedApi
internal val EmptySiQuantity = SiQuantity<Excluded, Excluded, Excluded, Excluded, Excluded, Excluded, Excluded>()

inline operator fun <
        reified Time2 : DecimalExponent,
        reified Length2 : DecimalExponent,
        reified Mass2 : DecimalExponent,
        reified ElectricCurrent2 : DecimalExponent,
        reified ThermodynamicTemperature2 : DecimalExponent,
        reified AmountOfSubstance2 : DecimalExponent,
        reified LuminousIntensity2 : DecimalExponent
        >
        Int.div(
    other: SiQuantity<Time2, Length2, Mass2, ElectricCurrent2, ThermodynamicTemperature2, AmountOfSubstance2, LuminousIntensity2>
): SiQuantity<Div<Excluded, Time2>, Div<Excluded, Length2>, Div<Excluded, Mass2>, Div<Excluded, ElectricCurrent2>, Div<Excluded, ThermodynamicTemperature2>, Div<Excluded, AmountOfSubstance2>, Div<Excluded, LuminousIntensity2>> {
    require(this == 1)
    return EmptySiQuantity / other
}
