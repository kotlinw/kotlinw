// Based on: https://github.com/huxi/sulky/blob/master/sulky-ulid/src/main/java/de/huxhorn/sulky/ulid/ULID.java

/*
 * sulky-modules - several general-purpose modules.
 * Copyright (C) 2007-2019 Joern Huxhorn
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * Copyright 2007-2019 Joern Huxhorn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.huxhorn.sulky.ulid

import kotlinx.datetime.Clock
import kotlin.random.Random

/*
 * https://github.com/ulid/spec
 */
object SulkyUlid {

    private val random: Random = Random.Default

    private fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

    fun appendULID(stringBuilder: StringBuilder) {
        internalAppendULID(stringBuilder, currentTimeMillis(), random)
    }

    fun nextULID(): String {
        return nextULID(currentTimeMillis())
    }

    fun nextULID(timestamp: Long): String {
        return internalUIDString(timestamp, random)
    }

    fun nextValue(timestamp: Long = currentTimeMillis()): Value {
        return internalNextValue(timestamp, random)
    }

    /**
     * Returns the next monotonic value. If an overflow happened while incrementing
     * the random part of the given previous ULID value then the returned value will
     * have a zero random part.
     *
     * @param previousUlid the previous ULID value.
     * @return the next monotonic value.
     */
    fun nextMonotonicValue(previousUlid: Value, timestamp: Long = currentTimeMillis()): Value {
        return if (previousUlid.timestamp() == timestamp) {
            previousUlid.increment()
        } else nextValue(timestamp)
    }

    /**
     * Returns the next monotonic value or empty if an overflow happened while incrementing
     * the random part of the given previous ULID value.
     *
     * @param previousUlid the previous ULID value.
     * @return the next monotonic value or empty if an overflow happened.
     */
    fun nextStrictlyMonotonicValue(previousUlid: Value): Value? {
        return nextStrictlyMonotonicValue(previousUlid, currentTimeMillis())
    }

    /**
     * Returns the next monotonic value or empty if an overflow happened while incrementing
     * the random part of the given previous ULID value.
     *
     * @param previousUlid the previous ULID value.
     * @param timestamp the timestamp of the next ULID value.
     * @return the next monotonic value or empty if an overflow happened.
     */
    fun nextStrictlyMonotonicValue(previousUlid: Value, timestamp: Long): Value? {
        val result = nextMonotonicValue(previousUlid, timestamp)
        return if (result.compareTo(previousUlid) < 1) {
            null
        } else {
            result
        }
    }

    class Value(
        /**
         * Returns the most significant 64 bits of this ULID's 128 bit value.
         *
         * @return  The most significant 64 bits of this ULID's 128 bit value
         */
        /*
                  * The most significant 64 bits of this ULID.
                  */
        val mostSignificantBits: Long,
        /**
         * Returns the least significant 64 bits of this ULID's 128 bit value.
         *
         * @return  The least significant 64 bits of this ULID's 128 bit value
         */
        /*
                  * The least significant 64 bits of this ULID.
                  */
        val leastSignificantBits: Long
    ) : Comparable<Value> {

        fun timestamp(): Long {
            return mostSignificantBits ushr 16
        }

        fun toBytes(): ByteArray {
            val result = ByteArray(16)
            for (i in 0..7) {
                result[i] = (mostSignificantBits shr (7 - i) * 8 and 0xFFL).toByte()
            }
            for (i in 8..15) {
                result[i] = (leastSignificantBits shr (15 - i) * 8 and 0xFFL).toByte()
            }
            return result
        }

        fun increment(): Value {
            val lsb = leastSignificantBits
            if (lsb != -0x1L) {
                return Value(mostSignificantBits, lsb + 1)
            }
            val msb = mostSignificantBits
            return if (msb and RANDOM_MSB_MASK != RANDOM_MSB_MASK) {
                Value(msb + 1, 0)
            } else Value(msb and TIMESTAMP_MSB_MASK, 0)
        }

        override fun hashCode(): Int {
            val hilo = mostSignificantBits xor leastSignificantBits
            return (hilo shr 32).toInt() xor hilo.toInt()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class !== other::class) return false
            val value = other as Value
            return (mostSignificantBits == value.mostSignificantBits
                    && leastSignificantBits == value.leastSignificantBits)
        }

        override operator fun compareTo(other: Value): Int {
            // The ordering is intentionally set up so that the ULIDs
            // can simply be numerically compared as two numbers
            return if (mostSignificantBits < other.mostSignificantBits) -1 else if (mostSignificantBits > other.mostSignificantBits) 1 else if (leastSignificantBits < other.leastSignificantBits) -1 else if (leastSignificantBits > other.leastSignificantBits) 1 else 0
        }

        override fun toString(): String {
            val buffer = CharArray(26)
            internalWriteCrockford(buffer, timestamp(), 10, 0)
            var value = mostSignificantBits and 0xFFFFL shl 24
            val interim = leastSignificantBits ushr 40
            value = value or interim
            internalWriteCrockford(buffer, value, 8, 10)
            internalWriteCrockford(buffer, leastSignificantBits, 8, 18)
            return buffer.concatToString()
        }

        companion object {
            private const val serialVersionUID = -3563159514112487717L
        }
    }

    private val ENCODING_CHARS = charArrayOf(
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K',
        'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'V', 'W', 'X',
        'Y', 'Z'
    )
    private val DECODING_CHARS = byteArrayOf( // 0
        -1, -1, -1, -1, -1, -1, -1, -1,  // 8
        -1, -1, -1, -1, -1, -1, -1, -1,  // 16
        -1, -1, -1, -1, -1, -1, -1, -1,  // 24
        -1, -1, -1, -1, -1, -1, -1, -1,  // 32
        -1, -1, -1, -1, -1, -1, -1, -1,  // 40
        -1, -1, -1, -1, -1, -1, -1, -1,  // 48
        0, 1, 2, 3, 4, 5, 6, 7,  // 56
        8, 9, -1, -1, -1, -1, -1, -1,  // 64
        -1, 10, 11, 12, 13, 14, 15, 16,  // 72
        17, 1, 18, 19, 1, 20, 21, 0,  // 80
        22, 23, 24, 25, 26, -1, 27, 28,  // 88
        29, 30, 31, -1, -1, -1, -1, -1,  // 96
        -1, 10, 11, 12, 13, 14, 15, 16,  // 104
        17, 1, 18, 19, 1, 20, 21, 0,  // 112
        22, 23, 24, 25, 26, -1, 27, 28,  // 120
        29, 30, 31
    )
    private const val MASK = 0x1F
    private const val MASK_BITS = 5
    private const val TIMESTAMP_OVERFLOW_MASK = -0x1000000000000L
    private const val TIMESTAMP_MSB_MASK = -0x10000L
    private const val RANDOM_MSB_MASK = 0xFFFFL
    fun parseULID(ulidString: String): Value {
        require(ulidString.length == 26) { "ulidString must be exactly 26 chars long." }
        val timeString = ulidString.substring(0, 10)
        val time = internalParseCrockford(timeString)
        require(time and TIMESTAMP_OVERFLOW_MASK == 0L) { "ulidString must not exceed '7ZZZZZZZZZZZZZZZZZZZZZZZZZ'!" }
        val part1String = ulidString.substring(10, 18)
        val part2String = ulidString.substring(18)
        val part1 = internalParseCrockford(part1String)
        val part2 = internalParseCrockford(part2String)
        val most = time shl 16 or (part1 ushr 24)
        val least = part2 or (part1 shl 40)
        return Value(most, least)
    }

    fun fromBytes(data: ByteArray): Value {
        require(data.size == 16) { "data must be 16 bytes in length!" }
        var mostSignificantBits: Long = 0
        var leastSignificantBits: Long = 0
        for (i in 0..7) {
            mostSignificantBits = mostSignificantBits shl 8 or (data[i].toInt() and 0xff).toLong()
        }
        for (i in 8..15) {
            leastSignificantBits = leastSignificantBits shl 8 or (data[i].toInt() and 0xff).toLong()
        }
        return Value(mostSignificantBits, leastSignificantBits)
    }

    /*
 * http://crockford.com/wrmg/base32.html
 */
    fun internalAppendCrockford(builder: StringBuilder, value: Long, count: Int) {
        for (i in count - 1 downTo 0) {
            val index = (value ushr i * MASK_BITS and MASK.toLong()).toInt()
            builder.append(ENCODING_CHARS[index])
        }
    }

    fun internalParseCrockford(input: String): Long {
        val length: Int = input.length
        require(length <= 12) { "input length must not exceed 12 but was $length!" }
        var result: Long = 0
        for (i in 0 until length) {
            val current: Char = input[i]
            var value: Byte = -1
            if (current.code < DECODING_CHARS.size) {
                value = DECODING_CHARS[current.code]
            }
            require(value >= 0) { "Illegal character '$current'!" }
            result = result or (value.toLong() shl (length - 1 - i) * MASK_BITS)
        }
        return result
    }

    /*
 * http://crockford.com/wrmg/base32.html
 */
    fun internalWriteCrockford(buffer: CharArray, value: Long, count: Int, offset: Int) {
        for (i in 0 until count) {
            val index = (value ushr (count - i - 1) * MASK_BITS and MASK.toLong()).toInt()
            buffer[offset + i] = ENCODING_CHARS[index]
        }
    }

    fun internalUIDString(timestamp: Long, random: Random): String {
        checkTimestamp(timestamp)
        val buffer = CharArray(26)
        internalWriteCrockford(buffer, timestamp, 10, 0)
        // could use nextBytes(byte[] bytes) instead
        internalWriteCrockford(buffer, random.nextLong(), 8, 10)
        internalWriteCrockford(buffer, random.nextLong(), 8, 18)
        return buffer.concatToString()
    }

    fun internalAppendULID(builder: StringBuilder, timestamp: Long, random: Random) {
        checkTimestamp(timestamp)
        internalAppendCrockford(builder, timestamp, 10)
        // could use nextBytes(byte[] bytes) instead
        internalAppendCrockford(builder, random.nextLong(), 8)
        internalAppendCrockford(builder, random.nextLong(), 8)
    }

    fun internalNextValue(timestamp: Long, random: Random): Value {
        checkTimestamp(timestamp)
        // could use nextBytes(byte[] bytes) instead
        var mostSignificantBits: Long = random.nextLong()
        val leastSignificantBits: Long = random.nextLong()
        mostSignificantBits = mostSignificantBits and 0xFFFFL
        mostSignificantBits = mostSignificantBits or (timestamp shl 16)
        return Value(mostSignificantBits, leastSignificantBits)
    }

    private fun checkTimestamp(timestamp: Long) {
        require(timestamp and TIMESTAMP_OVERFLOW_MASK == 0L) { "ULID does not support timestamps after +10889-08-02T05:31:50.655Z!" }
    }
}
