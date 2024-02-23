package kotlinw.util.stdlib.collection

/**
 * Drops the first [n] elements from the mutable list in-place.
 *
 * @param n The number of elements to drop from the list. Must be non-negative.
 * @throws IllegalArgumentException if [n] is negative.
 */
fun <E> MutableList<E>.dropInPlace(n: Int) {
    require(n >= 0)
    if (n > 0) {
        if (n >= size) {
            clear()
        } else {
            subList(0, n).clear()
        }
    }
}
