package kotlinw.util.stdlib

import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun Process.kill(
    normalTerminationTimeout: Duration = 4.seconds,
    forcedTerminationTimeout: Duration = 2.seconds
) {
    if (isAlive) {
        // TODO logger.debug { "Killing process pid=${pid()}..." }
        try {
            destroy()
            // TODO logger.debug { "Waiting for normal termination (at most for $normalTerminationTimeout) of pid=${pid()}..." }
            onExit().get(normalTerminationTimeout.inWholeMilliseconds, MILLISECONDS)
            // TODO logger.debug { "Process pid=${pid()} has been terminated normally." }
        } catch (e: Exception) {
            // TODO logger.debug { "Process pid=${pid()} did not terminated normally, trying to force it to exit (at most for $forcedTerminationTimeout)..." }
            try {
                destroyForcibly().waitFor(forcedTerminationTimeout.inWholeMilliseconds, MILLISECONDS)
            } catch (e: Exception) {
                if (isAlive) {
                    // TODO logger.debug { "Possibly failed to kill process pid=${pid()}." }
                } else {
                    // TODO logger.debug { "Process pid=${pid()} has been killed." }
                }
            }
        }
    }
}
