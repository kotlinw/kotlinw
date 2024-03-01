package xyz.kotlinw.module.pwa.server

import arrow.core.nonFatalOrThrow
import io.ktor.server.sessions.SessionStorage
import io.ktor.server.sessions.SessionStorageMemory
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.util.stdlib.infiniteLoop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock.System
import kotlinx.datetime.Instant
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.OnConstruction
import xyz.kotlinw.eventbus.inprocess.InProcessEventBus
import xyz.kotlinw.eventbus.inprocess.LocalEvent
import xyz.kotlinw.module.ktor.server.KtorServerApplicationConfigurer
import xyz.kotlinw.module.logging.LoggingModule
import xyz.kotlinw.module.pwa.server.WebAppSessionInvalidationReason.EXPLICIT
import xyz.kotlinw.module.pwa.server.WebAppSessionInvalidationReason.TIMEOUT
import java.util.concurrent.ConcurrentHashMap

enum class WebAppSessionInvalidationReason {
    TIMEOUT,
    EXPLICIT
}

sealed interface WebAppSessionEvent

data class BeforeWebAppSessionInvalidatedEvent(val sessionId: String, val sessionEncodedValue: String, val reason: WebAppSessionInvalidationReason) :
    LocalEvent(), WebAppSessionEvent

data class WebAppSessionInvalidatedEvent(val sessionId: String, val reason: WebAppSessionInvalidationReason) :
    LocalEvent(), WebAppSessionEvent

interface WebAppSessionStorageManager : SessionStorage {

    val activeSessionIds: Set<String>
}

@Component
class WebAppSessionStorageManagerImpl(
    private val eventBus: InProcessEventBus<LocalEvent>,
    private val sessionStorageBackendProvider: SessionStorageBackendProvider?
) : KtorServerApplicationConfigurer(), WebAppSessionStorageManager {

    private val logger = LoggingModule.loggerFactory.getLogger()

    private lateinit var sessionStorageBackend: SessionStorage

    // TODO ez így tök hibás, mert szerver újraindulás esetén reset-elődik az aktivitási időpont, azaz a szerver leállása és újraindása között lejárt session-ök aktívak maradnak (ráadásul a maximális, sessionTimeout érvényességgel)
    private val sessionLastActivityMap = ConcurrentHashMap<String, Instant>()

    private val sessionTimeout = 30.minutes // FIXME config

    override val activeSessionIds: Set<String> get() = sessionLastActivityMap.keys

    @OnConstruction
    suspend fun onConstruction() {
        sessionStorageBackend =
            if (sessionStorageBackendProvider != null) {
                sessionStorageBackendProvider.createSessionStorageBackend()
            } else {
                logger.warning { "Using default session storage: " / SessionStorageMemory::class.simpleName }
                SessionStorageMemory()
            }
    }

    override fun Context.setup() {
        ktorServerCoroutineScope.launch {
            infiniteLoop {
                delay(1.seconds) // TODO configurable

                logger.trace { "Checking session expiration..." }
                try {
                    val iterator = sessionLastActivityMap.iterator()
                    while (iterator.hasNext()) {
                        val (sessionId, lastActivityTimestamp) = iterator.next()
                        if (lastActivityTimestamp + sessionTimeout < System.now()) {
                            invalidateSessionStorageBackend(sessionId, TIMEOUT)
                            iterator.remove()
                        }
                    }
                } catch (e: Exception) {
                    logger.warning(e.nonFatalOrThrow()) { "Session expiration loop failed." }
                }
            }
        }
    }

    private suspend fun invalidateSessionStorageBackend(sessionId: String, reason: WebAppSessionInvalidationReason) {
        try {
            logger.debug { "Invalidating session: " / listOf(named("sessionId", sessionId), named("reason", reason)) }
            eventBus.publish(BeforeWebAppSessionInvalidatedEvent(sessionId, sessionStorageBackend.read(sessionId), reason))
            sessionStorageBackend.invalidate(sessionId)
        } catch (e: Exception) {
            logger.warning(e.nonFatalOrThrow()) {
                "Session invalidation failed: " / named("sessionId", sessionId)
            }
        }

        eventBus.publish(WebAppSessionInvalidatedEvent(sessionId, reason))
    }

    override suspend fun invalidate(id: String) {
        invalidateSessionStorageBackend(id, EXPLICIT)
        sessionLastActivityMap.remove(id)
    }

    override suspend fun read(id: String): String {
        val value = sessionStorageBackend.read(id)
        logger.trace {
            "Read session: " / mapOf("sessionId" to id, "sessionValue" to value)
        }

        sessionLastActivityMap[id] = System.now()

        return value
    }

    override suspend fun write(id: String, value: String) {
        if (logger.isTraceEnabled) {
            logger.trace { "Write session: " / mapOf("sessionId" to id, "sessionValue" to value) }
        } else {
            logger.debug { "Write session: " / named("sessionId", id) }
        }

        sessionStorageBackend.write(id, value)

        sessionLastActivityMap[id] = System.now()
    }
}
