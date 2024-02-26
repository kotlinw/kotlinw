package xyz.kotlinw.util.unit

import xyz.kotlinw.util.unit.DecimalExponent.ConstantDecimalExponent.P1
import xyz.kotlinw.util.unit.DecimalExponent.Excluded

object SiBaseQuantity {

    data object Time :
        SiQuantity<P1, Excluded, Excluded, Excluded, Excluded, Excluded, Excluded> by SiQuantity<P1, Excluded, Excluded, Excluded, Excluded, Excluded, Excluded>()

    data object Length :
        SiQuantity<Excluded, P1, Excluded, Excluded, Excluded, Excluded, Excluded> by SiQuantity<Excluded, P1, Excluded, Excluded, Excluded, Excluded, Excluded>()

    data object Mass :
        SiQuantity<Excluded, Excluded, P1, Excluded, Excluded, Excluded, Excluded> by SiQuantity<Excluded, Excluded, P1, Excluded, Excluded, Excluded, Excluded>()

    data object ElectricCurrent :
        SiQuantity<Excluded, Excluded, Excluded, P1, Excluded, Excluded, Excluded> by SiQuantity<Excluded, Excluded, Excluded, P1, Excluded, Excluded, Excluded>()

    data object ThermodynamicTemperature :
        SiQuantity<Excluded, Excluded, Excluded, Excluded, P1, Excluded, Excluded> by SiQuantity<Excluded, Excluded, Excluded, Excluded, P1, Excluded, Excluded>()

    data object AmountOfSubstance :
        SiQuantity<Excluded, Excluded, Excluded, Excluded, Excluded, P1, Excluded> by SiQuantity<Excluded, Excluded, Excluded, Excluded, Excluded, P1, Excluded>()

    data object LuminousIntensity :
        SiQuantity<Excluded, Excluded, Excluded, Excluded, Excluded, Excluded, P1> by SiQuantity<Excluded, Excluded, Excluded, Excluded, Excluded, Excluded, P1>()
}
