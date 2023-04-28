package kotlinw.logging.api

enum class LogLevel {

    Error,
    Warning,
    Info,
    Debug,
    Trace;

    val conventionalName get() = name.uppercase()
}
