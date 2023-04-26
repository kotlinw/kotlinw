package kotlinw.util.stdlib

import okio.BufferedSink

class ByteArrayView(
    internal val source: ByteArray,
    internal val startIndex: Int = 0,
    internal val endIndexExclusive: Int = source.size
) {

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

    operator fun iterator() =
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

fun ByteArray.view() = ByteArrayView(this)

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

fun ByteArrayView.toReadOnlyByteArray() =
    if (startIndex == 0 && endIndexExclusive == source.size) {
        source
    } else {
        ByteArray(size).also { copyInto(it) }
    }

fun BufferedSink.write(byteArrayView: ByteArrayView): BufferedSink =
    write(byteArrayView.source, byteArrayView.startIndex, byteArrayView.size)
