package kotlinw.configuration.core

import kotlinw.util.stdlib.HasPriority

class ConfigurationPropertyKeySegment(internal val value: String) {

    init {
        require(!(value.contains('\'') && value.contains('\"')))
    }

    val quoteCharacter: Char? =
        if (value.contains('\''))
            '\"'
        else if (value.contains('\"'))
            '\''
        else if (value.contains('.'))
            '\"'
        else
            null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConfigurationPropertyKeySegment) return false

        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "ConfigurationPropertyKeySegment(value='$value')"
    }
}

// TODO implement a more correct and robust parser
private fun parseConfigurationPropertyKey(name: String) =
    buildList {
        val segmentBuffer = StringBuilder()
        var inQuotedPart = false

        fun addSegment() {
            if (segmentBuffer.isNotEmpty()) {
                add(ConfigurationPropertyKeySegment(segmentBuffer.toString()))
                segmentBuffer.clear()
            }
        }

        name.forEach {
            if (it == '.') {
                if (inQuotedPart) {
                    segmentBuffer.append('.')
                } else {
                    addSegment()
                }
            } else if (it == '\"' || it == '\'') {
                inQuotedPart = !inQuotedPart
            } else {
                segmentBuffer.append(it)
            }
        }

        addSegment()
    }

class ConfigurationPropertyKey(private val segments: List<ConfigurationPropertyKeySegment>) {

    constructor(name: String) : this(parseConfigurationPropertyKey(name))

    val name =
        buildString {
            segments.forEachIndexed { index, segment ->
                if (index > 0) {
                    append('.')
                }

                val quoteCharacter = segment.quoteCharacter

                if (quoteCharacter != null) {
                    append(quoteCharacter)
                }

                append(segment.value)

                if (quoteCharacter != null) {
                    append(quoteCharacter)
                }
            }
        }

    fun startsWith(prefix: ConfigurationPropertyKey): Boolean {
        return name.startsWith(prefix.name)
    }

    fun subKeyAfterPrefix(prefix: ConfigurationPropertyKey): ConfigurationPropertyKey {
        return ConfigurationPropertyKey(segments.subList(prefix.segments.size, segments.size))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConfigurationPropertyKey) return false

        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString() = name
}

interface ConfigurationPropertySource : HasPriority {

    fun getPropertyValue(key: ConfigurationPropertyKey): Any?
}

interface EnumerableConfigurationPropertySource : ConfigurationPropertySource {

    fun getPropertyKeys(): Set<ConfigurationPropertyKey>
}
