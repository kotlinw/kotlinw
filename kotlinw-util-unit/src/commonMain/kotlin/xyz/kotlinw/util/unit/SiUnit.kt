package xyz.kotlinw.util.unit

import xyz.kotlinw.util.unit.DecimalExponent.Div

sealed class SiUnitFactor(val factorExponent: Int) {

    data object kilo : SiUnitFactor(3)
}

sealed interface SiUnit<Q : SiQuantity<*, *, *, *, *>> {

    val quantity: Q
}

fun <F: SiUnitFactor, U: SiUnit<Q>, Q : SiQuantity<*, *, *, *, *>> U.scale(factor: F) : ScaledSiUnit<F, U, Q> =
    ScaledSiUnitImpl(quantity, factor)

sealed interface ScaledSiUnit<
        Factor : SiUnitFactor,
        ScaledUnit : SiUnit<Q>,
        Q : SiQuantity<*, *, *, *, *>,
        > :
    SiUnit<Q> {

    val factor: Factor
}

@PublishedApi
internal class ScaledSiUnitImpl<
        Factor : SiUnitFactor,
        ScaledUnit : SiUnit<Q>,
        Q : SiQuantity<*, *, *, *, *>,
        >(
    override val quantity: Q, override val factor: Factor
) :
    ScaledSiUnit<Factor, ScaledUnit, Q>

@PublishedApi
internal class SiUnitImpl<
        Q : SiQuantity<*, *, *, *, *>
        >(
    override val quantity: Q
) :
    SiUnit<Q>
