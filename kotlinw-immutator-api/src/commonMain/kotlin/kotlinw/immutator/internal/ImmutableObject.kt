package kotlinw.immutator.internal

interface ImmutableObject<out MutableType> {
    fun _immutator_convertToMutable(): MutableType
}
