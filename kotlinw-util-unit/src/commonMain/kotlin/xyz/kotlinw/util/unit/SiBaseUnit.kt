package xyz.kotlinw.util.unit

import xyz.kotlinw.util.unit.SiBaseQuantity.AmountOfSubstance
import xyz.kotlinw.util.unit.SiBaseQuantity.ElectricCurrent
import xyz.kotlinw.util.unit.SiBaseQuantity.Length
import xyz.kotlinw.util.unit.SiBaseQuantity.LuminousIntensity
import xyz.kotlinw.util.unit.SiBaseQuantity.Mass
import xyz.kotlinw.util.unit.SiBaseQuantity.ThermodynamicTemperature
import xyz.kotlinw.util.unit.SiBaseQuantity.Time

sealed class SiBaseUnit<
        Q : SiQuantity<*, *, *, *, *, *, *>
        > :
    SiUnit<Q> {

    data object second : SiUnit<Time> by SiUnitImpl(Time)

    data object meter : SiUnit<Length> by SiUnitImpl(Length)

    data object kilogram : SiUnit<Mass> by SiUnitImpl(Mass)

    data object ampere : SiUnit<ElectricCurrent> by SiUnitImpl(ElectricCurrent)

    data object kelvin : SiUnit<ThermodynamicTemperature> by SiUnitImpl(ThermodynamicTemperature)

    data object mole : SiUnit<AmountOfSubstance> by SiUnitImpl(AmountOfSubstance)

    data object candela : SiUnit<LuminousIntensity> by SiUnitImpl(LuminousIntensity)
}
