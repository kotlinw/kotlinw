package xyz.kotlinw.util.unit

sealed class ValueWithUnit<
        N : Number,
        U : SiUnit<Q>,
        Q : SiQuantity<*, *, *, *, *, *, *>,
        >(
    val value: N,
    val unit: U
) : Number() {

    override fun toByte(): Byte = value.toByte()

    override fun toDouble(): Double = value.toDouble()

    override fun toFloat(): Float = value.toFloat()

    override fun toInt(): Int = value.toInt()

    override fun toLong(): Long = value.toLong()

    override fun toShort(): Short = value.toShort()

//    operator fun <
//            U2 : SiUnit<Q2>,
//            Q2 : SiQuantity<*, *, *, *, *, *, *>,
//            QR : SiQuantity<*, *, *, *, *, *, *>
//            > div(other: ValueWithUnit<N, U2, Q2>): ValueWithUnit<N, SiUnit<QR>, QR> =
//        performDiv(other.value, SiUnitImpl(unit.quantity / other.unit.quantity))

    abstract fun <U : SiUnit<Q>, Q : SiQuantity<*, *, *, *, *, *, *>> performDiv(
        otherValue: N,
        unit: U
    ): ValueWithUnit<N, U, Q>

    class DoubleValueWithUnit<
            U : SiUnit<Q>,
            Q : SiQuantity<*, *, *, *, *, *, *>,
            >(
        value: Double,
        unit: U
    ) :
        ValueWithUnit<Double, U, Q>(value, unit) {

        override fun <U : SiUnit<Q>, Q : SiQuantity<*, *, *, *, *, *, *>> performDiv(
            otherValue: Double,
            unit: U
        ) =
            DoubleValueWithUnit(value / otherValue, unit)
    }
}
