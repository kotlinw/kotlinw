package xyz.kotlinw.util.unit

import xyz.kotlinw.util.unit.SiBaseQuantity.AmountOfSubstance
import xyz.kotlinw.util.unit.SiBaseQuantity.ElectricCurrent
import xyz.kotlinw.util.unit.SiBaseQuantity.Length
import xyz.kotlinw.util.unit.SiBaseQuantity.LuminousIntensity
import xyz.kotlinw.util.unit.SiBaseQuantity.Mass
import xyz.kotlinw.util.unit.SiBaseQuantity.Time

object SiDerivedQuantity {

    // TODO továbbiak

    val Frequency =
        1 / Time

    val Area =
        Length * Length

    val Volume =
        Area * Length

    val Velocity =
        Length / Time

    val Acceleration =
        Velocity / Time

    val Force =
        Mass * Acceleration

    val Pressure =
        Force / Time

    val WaveNumber =
        1 / Length

    val MassDensity =
        Mass / Volume

    val SurfaceDensity =
        Mass / Area

    val SpecificVolume =
        Volume / Mass

    val CurrentDensity =
        ElectricCurrent / Area

    val MagneticFieldStrength =
        ElectricCurrent / Length

    val AmountOfSubstanceConcentration =
        AmountOfSubstance / Volume

    val MassConcentration =
        Mass / Volume

    val Luminance =
        LuminousIntensity / Area
}
