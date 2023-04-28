package kotlinw.logging.api

import kotlinw.logging.spi.LogMessageProvider
import kotlinw.logging.spi.buildLogMessage

private fun Logger.log(logLevel: LogLevel, messageProvider: LogMessageProvider) {
    log(logLevel, null, buildLogMessage(messageProvider), emptyList())
}

fun Logger.trace(messageProvider: LogMessageProvider) = log(LogLevel.Trace, messageProvider)

fun Logger.debug(messageProvider: LogMessageProvider) = log(LogLevel.Debug, messageProvider)

fun Logger.info(messageProvider: LogMessageProvider) = log(LogLevel.Info, messageProvider)

fun Logger.warning(messageProvider: LogMessageProvider) = log(LogLevel.Warning,messageProvider)

fun Logger.error(messageProvider: LogMessageProvider) = log(LogLevel.Error, messageProvider)

private fun Logger.log(logLevel: LogLevel, messageProvider: () -> String) {
    log(logLevel, null, LogMessage.SimpleText(messageProvider()), emptyList())
}

fun Logger.trace(messageProvider: () -> String) = log(LogLevel.Trace, messageProvider)

fun Logger.debug(messageProvider: () -> String) = log(LogLevel.Debug, messageProvider)

fun Logger.info(messageProvider: () -> String) = log(LogLevel.Info, messageProvider)

fun Logger.warning(messageProvider: () -> String) = log(LogLevel.Warning, messageProvider)

fun Logger.error(messageProvider: () -> String) = log(LogLevel.Error, messageProvider)

private fun Logger.log(logLevel: LogLevel, message: String) {
    log(logLevel, null, LogMessage.SimpleText(message), emptyList())
}

fun Logger.trace(message: String) = log(LogLevel.Trace, message)

fun Logger.debug(message:String) = log(LogLevel.Debug, message)

fun Logger.info(message:  String) = log(LogLevel.Info, message)

fun Logger.warning(message: String) = log(LogLevel.Warning, message)

fun Logger.error(message:  String) = log(LogLevel.Error, message)
