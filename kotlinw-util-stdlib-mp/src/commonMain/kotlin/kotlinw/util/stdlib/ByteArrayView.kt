package kotlinw.util.stdlib

/**
 * Read-only view of a [ByteArray].
 *
 * Note: the byte array must not be modified while there are [ByteArrayView] instances referencing it.
 */
@DelicateKotlinwApi
class ByteArrayView private constructor(
    internal val source: ByteArray,
    internal val startIndex: Int = 0,
    internal val endIndexExclusive: Int = source.size
) : Iterable<Byte> {

    companion object {

        @DelicateKotlinwApi
        fun ByteArray.view() = ByteArrayView(this)

        @DelicateKotlinwApi
        fun ByteArray.view(startIndex: Int = 0, endIndexExclusive: Int = size) =
            ByteArrayView(this, startIndex, endIndexExclusive)

        @DelicateKotlinwApi
        fun ByteArrayView.view(startIndex: Int = 0, endIndexExclusive: Int = size) =
            ByteArrayView(source, this.startIndex + startIndex, this.startIndex + endIndexExclusive)

        fun ByteArrayView.decodeToString() = source.decodeToString(startIndex, endIndexExclusive)

        /**
         * See: [ByteArray.copyInto]
         */
        fun ByteArrayView.copyInto(
            destination: ByteArray,
            destinationOffset: Int = 0,
            startIndex: Int = 0,
            endIndex: Int = size
        ) = source.copyInto(destination, destinationOffset, this.startIndex + startIndex, this.startIndex + endIndex)

        @DelicateKotlinwApi
        fun ByteArrayView.toReadOnlyByteArray() =
            if (startIndex == 0 && endIndexExclusive == source.size) {
                source
            } else {
                ByteArray(size).also { copyInto(it) }
            }
    }

    init {
        require(startIndex in source.indices)
        require(startIndex <= endIndexExclusive)
        require(endIndexExclusive <= source.size)
    }

    val size: Int = endIndexExclusive - startIndex

    operator fun get(index: Int): Byte {
        require(index <= 0 && index < size)
        return source[startIndex + index]
    }

    override operator fun iterator() =
        object : ByteIterator() {

            var index: Int = startIndex

            override fun nextByte(): Byte {
                if (!hasNext()) throw NoSuchElementException("$index")
                return source[index++]
            }

            override operator fun hasNext(): Boolean {
                return index < source.size
            }
        }
}
