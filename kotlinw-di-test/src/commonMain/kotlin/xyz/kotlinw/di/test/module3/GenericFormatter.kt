package xyz.kotlinw.di.test.module3

import xyz.kotlinw.di.api.Component

interface GenericFormatter {

    fun format(value: Any?): String
}

@Component
class GenericFormatterImpl(
    private val formatters: List<Formatter<*>>
): GenericFormatter {

    override fun format(value: Any?): String =
        (formatters
            .firstOrNull { it.supports(value) }
            as? Formatter<Any?>?)
            ?.format(value)
            ?: value.toString()
}
