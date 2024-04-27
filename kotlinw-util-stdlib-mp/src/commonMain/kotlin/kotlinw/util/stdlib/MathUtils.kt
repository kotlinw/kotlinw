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
    previousAverage + ((currentValue - previousAverage) / (previousSampleCount + 1.0))

sealed interface IncrementalAverageHolder {

    val average: Double?

    val sampleCount: Int

    data class ImmutableIncrementalAverageHolder private constructor(
        override val sampleCount: Int,
        override val average: Double?
    ) :
        IncrementalAverageHolder {

        companion object {

            val Empty = ImmutableIncrementalAverageHolder(0.0, 0)
        }

        constructor() : this(0, null)

        constructor(average: Double, sampleCount: Int) : this(sampleCount, average)

        fun addValue(value: Double) =
            ImmutableIncrementalAverageHolder(
                if (sampleCount == 0) value else incrementalAverage(average!!, sampleCount, value),
                sampleCount + 1
            )
    }

    @NonThreadSafe
    class MutableIncrementalAverageHolder private constructor(
        override var sampleCount: Int,
        override var average: Double?
    ) : IncrementalAverageHolder {

        constructor() : this(0, null)

        constructor(average: Double, sampleCount: Int) : this(sampleCount, average)

        fun addValue(value: Double) {
            average = if (sampleCount == 0) value else incrementalAverage(average!!, sampleCount++, value)
        }
    }
}
