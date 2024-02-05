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
