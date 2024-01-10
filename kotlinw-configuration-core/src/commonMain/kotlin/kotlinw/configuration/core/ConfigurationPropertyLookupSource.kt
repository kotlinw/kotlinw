package kotlinw.configuration.core

import kotlinw.util.stdlib.HasPriority
import kotlinw.util.stdlib.Priority

interface ConfigurationPropertyLookupSource : HasPriority {

    suspend fun reload()

    fun getPropertyValueOrNull(key: ConfigurationPropertyKey): EncodedConfigurationPropertyValue?
}

class ConfigurationPropertyLookupSourceImpl(
    private val resolver: ConfigurationPropertyResolver,
    override val priority: Priority = Priority.Normal
) : ConfigurationPropertyLookupSource, HasPriority {

    override suspend fun reload() {
        resolver.reload()
    }

    override fun getPropertyValueOrNull(key: ConfigurationPropertyKey): EncodedConfigurationPropertyValue? =
        resolver.getPropertyValueOrNull(key)
}

interface EnumerableConfigurationPropertyLookupSource : ConfigurationPropertyLookupSource {

    fun getPropertyKeys(): Set<ConfigurationPropertyKey>
}

class EnumerableConfigurationPropertyLookupSourceImpl(
    private val resolver: EnumerableConfigurationPropertyResolver,
    override val priority: Priority = Priority.Normal
) : EnumerableConfigurationPropertyLookupSource, HasPriority {

    override suspend fun reload() {
        resolver.reload()
    }

    override fun getPropertyKeys(): Set<ConfigurationPropertyKey> = resolver.getPropertyKeys()

    override fun getPropertyValueOrNull(key: ConfigurationPropertyKey): EncodedConfigurationPropertyValue? =
        resolver.getPropertyValueOrNull(key)

    override fun toString(): String {
        return "EnumerableConfigurationPropertyLookupSourceImpl(resolver=$resolver, priority=$priority)"
    }
}

class ConfigurationPropertyKeySegment(internal val value: String) {

    init {
        require(!(value.contains('\'') && value.contains('\"')))
    }

    internal val quoteCharacter: Char? =
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

class ConfigurationPropertyKey(
    private val segments: List<ConfigurationPropertyKeySegment>,
    val sourceInfo: String? = null
) {

    constructor(name: String, sourceInfo: String? = null) :
            this(parseConfigurationPropertyKey(name), sourceInfo)

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
        // assert(startsWith(prefix))
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

    override fun toString() = if (sourceInfo != null) "$name (source: $sourceInfo)" else name
}

fun ConfigurationPropertyKey.startsWith(prefix: String) = startsWith(ConfigurationPropertyKey(prefix))
