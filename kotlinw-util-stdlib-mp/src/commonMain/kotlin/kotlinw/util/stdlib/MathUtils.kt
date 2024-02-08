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

sealed interface IncrementalAverageHolder {

    val average: Double

    val sampleCount: Int

    data class ImmutableIncrementalAverageHolder(override val average: Double, override val sampleCount: Int) :
        IncrementalAverageHolder {

        companion object {

            val Empty = ImmutableIncrementalAverageHolder(0.0, 0)
        }

        fun addValue(value: Double) =
            ImmutableIncrementalAverageHolder(
                incrementalAverage(average, sampleCount, value),
                sampleCount + 1
            )
    }

    @NonThreadSafe
    class MutableIncrementalAverageHolder private constructor(
        override var average: Double,
        override var sampleCount: Int
    ) : IncrementalAverageHolder {

        constructor() : this(0.0, 0)

        fun addValue(value: Double) {
            average = incrementalAverage(average, sampleCount++, value)
        }
    }
}
