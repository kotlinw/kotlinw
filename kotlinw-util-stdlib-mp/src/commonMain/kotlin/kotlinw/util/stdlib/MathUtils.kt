package kotlinw.util.stdlib

import kotlin.math.pow
import kotlin.math.roundToInt

fun Double.round(decimalPlaces: Int): Double {
    val factor = 10.0.pow(decimalPlaces)
    return (this * factor).roundToInt() / factor
}

fun Float.round(decimalPlaces: Int): Float {
    val factor = 10F.pow(decimalPlaces)
    return (this * factor).roundToInt() / factor
}

fun incrementalAverage(previousAverage: Double, previousSampleCount: Int, currentValue: Double) =
    previousAverage + ((currentValue - previousAverage) / (previousSampleCount + 1F))

data class IncrementalAverageHolder(val average: Double, val sampleCount: Int) {

    companion object {

        val Empty = IncrementalAverageHolder(0.0, 0)
    }

    fun addValue(value: Double) =
        IncrementalAverageHolder(
            incrementalAverage(average, sampleCount, value),
            sampleCount + 1
        )
}
