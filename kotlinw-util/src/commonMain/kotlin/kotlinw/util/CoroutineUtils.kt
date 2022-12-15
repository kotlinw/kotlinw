package kotlinw.util

import kotlinx.coroutines.delay
import kotlin.time.Duration

// TODO ezt egy kotlinw-coroutine-util-mp projektbe
// TODO log exception
suspend inline fun <T> runUntilNoExceptionThrown(
    delayAfterException: Duration = Duration.ZERO,
    noinline logEmitter: ((Exception) -> Any?)? = null, // TODO Logger-t kapjon receiver-ként
    block: () -> T
): T {
    do {
        try {
            return block()
        } catch (e: Exception) {
            delay(delayAfterException)
        }
    } while (true)
}
