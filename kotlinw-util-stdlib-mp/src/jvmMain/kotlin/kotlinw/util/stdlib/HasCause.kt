package kotlinw.util.stdlib

interface HasCause {
    val cause: Throwable
}

fun Any.getCauseOrNull(): Throwable? =
    when {
        this is HasCause -> cause
        this is Throwable && this.cause != null -> this.cause!!
        else -> null
    }
