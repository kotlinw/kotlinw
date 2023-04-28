package kotlinw.logging.api

interface LoggerFactory {

    fun getLogger(loggerName: String): Logger
}
