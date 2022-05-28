package kotlinw.immutator.api

interface ImmutableObject<out MutableType> {
    fun toMutable(): MutableType
}
