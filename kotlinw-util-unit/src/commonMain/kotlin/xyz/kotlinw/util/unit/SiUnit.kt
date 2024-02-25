package xyz.kotlinw.util.unit

sealed class SiUnit(override val symbol: String) : MeasurementUnit {

    data object Meter: SiUnit("m")

    data object Kilogram: SiUnit("kg")

    data object Second: SiUnit("s")

    data object Ampere: SiUnit("A")

    data object Kelvin: SiUnit("K")

    data object Mole: SiUnit("mol")

    data object Candela: SiUnit("cd")
}
