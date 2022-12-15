package kotlinw.util

import mu.KotlinLogging
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

fun Process.kill(
    normalTerminationTimeout: Duration = 4.seconds,
    forcedTerminationTimeout: Duration = 2.seconds
) {
    if (isAlive) {
        logger.debug { "Killing process pid=${pid()}..." }
        try {
            destroy()
            logger.debug { "Waiting for normal termination (at most for $normalTerminationTimeout) of pid=${pid()}..." }
            onExit().get(normalTerminationTimeout.inWholeMilliseconds, MILLISECONDS)
            logger.debug { "Process pid=${pid()} has been terminated normally." }
        } catch (e: Exception) {
            logger.debug { "Process pid=${pid()} did not terminated normally, trying to force it to exit (at most for $forcedTerminationTimeout)..." }
            try {
                destroyForcibly().waitFor(forcedTerminationTimeout.inWholeMilliseconds, MILLISECONDS)
            } catch (e: Exception) {
                if (isAlive) {
                    logger.debug { "Possibly failed to kill process pid=${pid()}." }
                } else {
                    logger.debug { "Process pid=${pid()} has been killed." }
                }
            }
        }
    }
}
