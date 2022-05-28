package kotlinw.immutator.api

import kotlinx.collections.immutable.ImmutableList

interface ConfinedMutableList<ElementMutableType, ElementImmutableType> :
    MutableList<ElementMutableType>, MutableObject<ImmutableList<ElementImmutableType>> {

    fun <T> mutate(mutator: (MutableList<ElementMutableType>) -> T): T
}

fun <ElementMutableType, ElementImmutableType : ImmutableObject<ElementMutableType>>
        ConfinedMutableList<ElementMutableType, ElementImmutableType>.add(element: ElementImmutableType): Boolean =
    add(element.toMutable())
