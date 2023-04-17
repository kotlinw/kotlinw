package kotlinw.template

import kotlinw.util.stdlib.BitSet

fun main() {
    val bitSet = BitSet(8)
    bitSet.set(5, true)
    println(bitSet)
}
