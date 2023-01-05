package kotlinw.util.stdlib

fun String.ifBlankNull() = ifBlank { null }

fun String.splitByNewLines(excludeBlankLines: Boolean = true): List<String> =
    split("\r\n", "\n").let { lines ->
        if (excludeBlankLines) {
            lines.filter { it.isNotBlank() }
        } else {
            lines
        }
    }
