package xyz.kotlinw.util.unit

import kotlin.test.Test
import kotlin.test.assertEquals
import xyz.kotlinw.util.unit.DecimalExponent.ConstantDecimalExponent.M2
import xyz.kotlinw.util.unit.DecimalExponent.ConstantDecimalExponent.P1
import xyz.kotlinw.util.unit.DecimalExponent.ConstantDecimalExponent.P3
import xyz.kotlinw.util.unit.DecimalExponent.Excluded
import xyz.kotlinw.util.unit.SiDerivedQuantity.Acceleration
import xyz.kotlinw.util.unit.SiDerivedQuantity.Area
import xyz.kotlinw.util.unit.SiDerivedQuantity.Force
import xyz.kotlinw.util.unit.SiDerivedQuantity.Frequency
import xyz.kotlinw.util.unit.SiDerivedQuantity.Velocity
import xyz.kotlinw.util.unit.SiDerivedQuantity.Volume

class SiQuantityTest {

    @Test
    fun testEquals() {
        assertEquals<Any>(
            Acceleration,
            SiQuantity<M2, P1, Excluded, Excluded, Excluded, Excluded, Excluded>()
        )

        assertEquals(
            SiBaseQuantity.Length * SiBaseQuantity.Length * SiBaseQuantity.Length,
            Area * SiBaseQuantity.Length
        )
        assertEquals(
            SiBaseQuantity.Length * SiBaseQuantity.Length * SiBaseQuantity.Length,
            Volume
        )
        assertEquals<Any>(
            SiBaseQuantity.Length * SiBaseQuantity.Length * SiBaseQuantity.Length,
            SiQuantity<Excluded, P3, Excluded, Excluded, Excluded, Excluded, Excluded>()
        )

        assertEquals<Any>(
            SiBaseQuantity.Mass * SiBaseQuantity.Length / (SiBaseQuantity.Time * SiBaseQuantity.Time),
            SiBaseQuantity.Mass * Acceleration
        )
    }

    @Test
    fun testVelocity() {
        assertEquals(-1, Velocity.timeExponent)
        assertEquals(1, Velocity.lengthExponent)
        assertEquals(null, Velocity.massExponent)
        assertEquals(null, Velocity.electricCurrentExponent)
        assertEquals(null, Velocity.thermodynamicTemperatureExponent)
        assertEquals(null, Velocity.amountOfSubstanceExponent)
    }

    @Test
    fun testAcceleration() {
        assertEquals(-2, Acceleration.timeExponent)
        assertEquals(1, Acceleration.lengthExponent)
        assertEquals(null, Acceleration.massExponent)
        assertEquals(null, Acceleration.electricCurrentExponent)
        assertEquals(null, Acceleration.thermodynamicTemperatureExponent)
        assertEquals(null, Acceleration.amountOfSubstanceExponent)
    }

    @Test
    fun testArea() {
        assertEquals(null, Area.timeExponent)
        assertEquals(2, Area.lengthExponent)
        assertEquals(null, Area.massExponent)
        assertEquals(null, Area.electricCurrentExponent)
        assertEquals(null, Area.thermodynamicTemperatureExponent)
        assertEquals(null, Area.amountOfSubstanceExponent)
    }

    @Test
    fun testVolume() {
        assertEquals(null, Volume.timeExponent)
        assertEquals(3, Volume.lengthExponent)
        assertEquals(null, Volume.massExponent)
        assertEquals(null, Volume.electricCurrentExponent)
        assertEquals(null, Volume.thermodynamicTemperatureExponent)
        assertEquals(null, Volume.amountOfSubstanceExponent)
    }

    @Test
    fun testForce() {
        assertEquals(-2, Force.timeExponent)
        assertEquals(1, Force.lengthExponent)
        assertEquals(1, Force.massExponent)
        assertEquals(null, Force.electricCurrentExponent)
        assertEquals(null, Force.thermodynamicTemperatureExponent)
        assertEquals(null, Force.amountOfSubstanceExponent)
    }

    @Test
    fun testFrequency() {
        assertEquals(-1, Frequency.timeExponent)
        assertEquals(null, Frequency.lengthExponent)
        assertEquals(null, Frequency.massExponent)
        assertEquals(null, Frequency.electricCurrentExponent)
        assertEquals(null, Frequency.thermodynamicTemperatureExponent)
        assertEquals(null, Frequency.amountOfSubstanceExponent)
    }
}
