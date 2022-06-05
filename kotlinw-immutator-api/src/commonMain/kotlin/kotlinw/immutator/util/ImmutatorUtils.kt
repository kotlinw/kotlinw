package kotlinw.immutator.util

import kotlinw.immutator.internal.ImmutableObject
import kotlinw.immutator.internal.MutableObject

inline fun <ImmutableType : ImmutableObject<MutableType>, MutableType : MutableObject<ImmutableType>> ImmutableType.mutate(
    mutator: (MutableType) -> Unit
): ImmutableType =
    _immutator_convertToMutable().let { mutable ->
        mutator(mutable)
        mutable._immutator_convertToImmutable()
    }
