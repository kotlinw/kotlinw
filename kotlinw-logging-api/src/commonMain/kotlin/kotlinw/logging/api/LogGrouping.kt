package kotlinw.logging.api

data class LogGrouping(val name: String, val parent: LogGrouping?) : LoggingContextFeature
