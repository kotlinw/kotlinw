package kotlinw.util.stdlib

import kotlin.random.Random

fun String.ifBlankNull() = ifBlank { null }

fun String.splitByNewLines(excludeBlankLines: Boolean = true): List<String> =
    split("\r\n", "\n").let { lines ->
        if (excludeBlankLines) {
            lines.filter { it.isNotBlank() }
        } else {
            lines
        }
    }

private val randomAlphanumericStringChars by lazy {
    (('a'..'z') + ('A'..'Z') + ('0'..'9')).toCharArray()
}

fun randomAlphanumericString(length: Int) = (1..length)
    .map { Random.nextInt(0, randomAlphanumericStringChars.size).let { randomAlphanumericStringChars[it] } }
    .joinToString("")
