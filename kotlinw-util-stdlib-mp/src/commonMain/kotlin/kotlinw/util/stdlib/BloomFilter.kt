package kotlinw.util.stdlib

interface BloomFilter<T : Any> {

    companion object

    fun mightContain(value: T): Boolean
}

fun <T : Any> newMutableBloomFilter(size: Int): MutableBloomFilter<T> = MutableBloomFilterImpl(size)

interface MutableBloomFilter<T : Any> : BloomFilter<T> {

    fun add(value: T)
}

fun <T : Any> MutableBloomFilter<T>.addAll(iterable: Iterable<T>) {
    iterable.forEach {
        add(it)
    }
}

private class MutableBloomFilterImpl<T : Any>(private val size: Int) : MutableBloomFilter<T> {

    init {
        require(size >= 1)
    }

    internal val bits = BitSet(size)

    override fun add(value: T) = bits.set(computeBitNumber(value))

    override fun mightContain(value: T) = bits[computeBitNumber(value)]

    private fun computeBitNumber(value: T) = value.hashCode() % size
}
