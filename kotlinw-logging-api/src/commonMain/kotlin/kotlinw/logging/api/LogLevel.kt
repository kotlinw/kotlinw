package kotlinw.logging.api

enum class LogLevel {

    Trace,
    Debug,
    Info,
    Warning,
    Error;

    val conventionalName get() = name.uppercase()
}
