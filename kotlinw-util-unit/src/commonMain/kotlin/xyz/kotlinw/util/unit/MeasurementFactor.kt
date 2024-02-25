package xyz.kotlinw.util.unit

interface MeasurementFactor<N : Number> {

    val symbol: String?

    fun apply(value: N): N
}

object UnitMeasurementFactor : MeasurementFactor<Number> {

    override val symbol: String? get() = null

    override fun apply(value: Number): Number = value
}
