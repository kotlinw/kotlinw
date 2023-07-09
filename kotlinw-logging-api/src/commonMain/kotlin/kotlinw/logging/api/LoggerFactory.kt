package kotlinw.logging.api

import kotlinw.logging.spi.UnknownLoggerName
import kotlinw.logging.spi.resolveLoggerName
import kotlinw.util.stdlib.debugName
import kotlin.reflect.KClass

interface LoggerFactory {

    companion object {

        @Suppress("NOTHING_TO_INLINE")
        inline fun LoggerFactory.getLogger() = getLogger(resolveLoggerName {} ?: UnknownLoggerName)
    }

    fun getLogger(loggerName: String): Logger

    fun getLogger(kClass: KClass<*>) = getLogger(kClass.debugName)
}
