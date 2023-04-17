package kotlinw.util.stdlib

import kotlin.math.ln

interface BloomFilter<T : Any> {

    companion object {

        /**
         * Computes m (total bits of Bloom filter) which is expected to achieve, for the specified
         * expected insertions, the required false positive probability.
         *
         * See http://en.wikipedia.org/wiki/Bloom_filter#Probability_of_false_positives for the
         * formula.
         *
         * Based on: https://github.com/google/guava/blob/master/guava/src/com/google/common/hash/BloomFilter.java#L532
         */
        internal fun calculateOptimalSize(expectedInsertions: Int, falsePositiveRate: Double): Int {
            require(expectedInsertions > 0)
            require(falsePositiveRate > 0.0 && falsePositiveRate < 1.0)
            return (-expectedInsertions * ln(falsePositiveRate) / (ln(2.0) * ln(2.0))).toInt()
        }
    }

    fun mightContain(value: T): Boolean
}

fun <T : Any> newMutableBloomFilter(expectedInsertions: Int, falsePositiveRate: Double = 0.01): MutableBloomFilter<T> =
    MutableBloomFilterImpl(expectedInsertions, falsePositiveRate)

interface MutableBloomFilter<T : Any> : BloomFilter<T> {

    fun add(value: T)
}

fun <T : Any> MutableBloomFilter<T>.addAll(iterable: Iterable<T>) {
    iterable.forEach {
        add(it)
    }
}

private class MutableBloomFilterImpl<T : Any>(
    expectedInsertions: Int,
    falsePositiveRate: Double
) :
    MutableBloomFilter<T> {

    init {
        require(expectedInsertions >= 1)
    }

    private val size =
        BloomFilter.calculateOptimalSize(expectedInsertions, falsePositiveRate).let { if (it == 0) 1 else it }

    val bits = BitSet(size)

    override fun add(value: T) = bits.set(computeBitNumber(value))

    override fun mightContain(value: T) = bits[computeBitNumber(value)]

    private fun computeBitNumber(value: T) = value.hashCode() % size
}
