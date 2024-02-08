package kotlinw.util.stdlib

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinw.util.stdlib.IncrementalAverageHolder.ImmutableIncrementalAverageHolder

class MathUtilsTest {

    @Test
    fun testRound_0() {
        assertEquals(35.0, 35.4.round(0))
        assertEquals(36.0, 35.5.round(0))
        assertEquals(36.0, 35.87334.round(0))
    }

    @Test
    fun testRound_3() {
        assertEquals(35.4, 35.4.round(3))
        assertEquals(35.5, 35.5.round(3))
        assertEquals(35.873, 35.87334.round(3))
    }

    private fun average(values: DoubleArray) = values.sum() / values.size

    private fun incrementalAverage(values: DoubleArray) =
        values.toList()
            .fold(ImmutableIncrementalAverageHolder.Empty) { incrementalAverageHolder, value ->
                incrementalAverageHolder.addValue(value)
            }
            .average

    private fun assertAverageResult(vararg values: Double) {
        assertEquals(average(values), incrementalAverage(values))
    }

    @Test
    fun testIncrementalAverage() {
        assertAverageResult(0.0)
        assertAverageResult(10.0)
        assertAverageResult(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0)
        assertAverageResult(10.0, 20.0, 20.0, 10.0)
    }
}
