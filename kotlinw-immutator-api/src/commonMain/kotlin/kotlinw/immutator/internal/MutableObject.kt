
package kotlinw.immutator.internal

interface MutableObject<out ImmutableType> {
    @Suppress("FunctionName")
    fun _immutator_convertToImmutable(): ImmutableType
}
