package kotlinw.immutator.api

import kotlinw.immutator.internal.ImmutableObject
import kotlinw.immutator.internal.MutableObject
import kotlinx.collections.immutable.ImmutableList

interface ConfinedMutableList<ElementMutableType, ElementImmutableType> :
    MutableList<ElementMutableType>, MutableObject<ImmutableList<ElementImmutableType>> {

    fun <T> mutate(mutator: (MutableList<ElementMutableType>) -> T): T
}

fun <ElementMutableType, ElementImmutableType : ImmutableObject<ElementMutableType>>
        ConfinedMutableList<ElementMutableType, ElementImmutableType>.add(element: ElementImmutableType): Boolean =
    add(element._immutator_convertToMutable())
