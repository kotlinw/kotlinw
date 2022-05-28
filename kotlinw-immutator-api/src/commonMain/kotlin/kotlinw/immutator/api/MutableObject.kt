package kotlinw.immutator.api

interface MutableObject<out ImmutableType> {
    fun toImmutable(): ImmutableType
}
