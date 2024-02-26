package xyz.kotlinw.util.unit

sealed class ScaledValue<N : Number> : Number() {

    abstract val value: N

    abstract val scale: MeasurementFactor<N>

    override fun toByte(): Byte = scaledValue.toByte()

    override fun toDouble(): Double = scaledValue.toDouble()

    override fun toFloat(): Float = scaledValue.toFloat()

    override fun toInt(): Int = scaledValue.toInt()

    override fun toLong(): Long = scaledValue.toLong()

    override fun toShort(): Short = scaledValue.toShort()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ScaledValue<*>) return false

        return scaledValue == other.scaledValue
    }

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + scale.hashCode()
        return result
    }

    val scaledValue get() = scale.apply(value)

    class UnitlessScaledValue<N : Number>(
        override val value: N,
        override val scale: MeasurementFactor<N>
    ) : ScaledValue<N>() {

        override fun toString() = scaledValue.toString()
    }

    class ValueWithUnit<N : Number, U : CoreUnit>(
        override val value: N,
        val unit: U
    ) : ScaledValue<N>() {

        override val scale: MeasurementFactor<N> get() = UnitMeasurementFactor as MeasurementFactor<N>

        override fun toString(): String = "$value ${unit.symbol}"
    }

    class ScaledValueWithUnit<N : Number, U : CoreUnit>(
        override val value: N,
        override val scale: MeasurementFactor<N>,
        val unit: U
    ) : ScaledValue<N>() {

        override fun toString() = buildString {
            append(value)
            append(' ')
            if (scale !is UnitMeasurementFactor) {
                append(scale.symbol)
            }
            append(unit.symbol)
        }
    }
}
