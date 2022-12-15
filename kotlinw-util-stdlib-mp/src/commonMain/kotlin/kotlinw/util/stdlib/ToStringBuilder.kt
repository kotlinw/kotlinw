package kotlinw.util.stdlib

import kotlin.reflect.KProperty0

class ToStringBuilder(private val obj: Any) {
    private val properties = mutableListOf<Pair<String, String>>()

    fun add(name: String, value: Any?): ToStringBuilder {
        properties.add(
            Pair(
                name,
                try {
                    value.toString()
                } catch (e: Exception) {
                    "<toString() failed>"
                }
            )
        )
        return this
    }

    fun add(property: KProperty0<*>): ToStringBuilder {
        add(
            property.name,
            try {
                property.get()
            } catch (e: Exception) {
                "<failed to get>"
            }
        )
        return this
    }

    override fun toString() = (obj::class.simpleName ?: "<unknown class>") +
            "(" + properties.joinToString(", ") { it.first + "=" + it.second } + ")"
}

fun ToStringBuilder.addAll(vararg properties: KProperty0<*>): ToStringBuilder {
    properties.forEach { add(it) }
    return this
}

interface HasToStringBuilder {
    fun buildToString(toStringBuilder: ToStringBuilder)
}

fun HasToStringBuilder.buildToString(): String = ToStringBuilder(this).let {
    buildToString(it)
    it.toString()
}
