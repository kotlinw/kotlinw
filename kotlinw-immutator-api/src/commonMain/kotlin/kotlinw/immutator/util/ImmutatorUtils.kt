package kotlinw.immutator.util

import kotlinw.immutator.api.ImmutableObject
import kotlinw.immutator.api.MutableObject
import kotlinx.datetime.LocalDate

inline fun <ImmutableType : ImmutableObject<MutableType>, MutableType : MutableObject<ImmutableType>> ImmutableType.mutate(
    mutator: (MutableType) -> Unit
): ImmutableType =
    toMutable().let { mutable ->
        mutator(mutable)
        mutable.toImmutable()
    }

fun LocalDate.toImmutable() = this

fun String.toImmutable() = this
