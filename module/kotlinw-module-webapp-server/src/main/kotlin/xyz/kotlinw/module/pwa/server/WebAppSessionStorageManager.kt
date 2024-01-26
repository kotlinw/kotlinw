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
import xyz.kotlinw.module.ktor.server.KtorServerApplicationConfigurer
import xyz.kotlinw.module.logging.LoggingModule
import java.util.concurrent.ConcurrentHashMap

interface WebAppSessionStorageManager : SessionStorage {

    val activeSessionIds: Set<String>
}

@Component
class WebAppSessionStorageManagerImpl(
    private val sessionStorageBackendProvider: SessionStorageBackendProvider?
) : KtorServerApplicationConfigurer(), WebAppSessionStorageManager {

    private val logger = LoggingModule.loggerFactory.getLogger()

    private lateinit var sessionStorageBackend: SessionStorage

    private val sessionLastActivityMap = ConcurrentHashMap<String, Instant>()

    private val sessionTimeout = 30.minutes // FIXME config

    override val activeSessionIds: Set<String> get() = sessionLastActivityMap.keys

    @OnConstruction
    fun onConstruction() {
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
                            iterator.remove()

                            try {
                                sessionStorageBackend.invalidate(sessionId)
                            } catch (e: Exception) {
                                logger.warning(e.nonFatalOrThrow()) {
                                    "Session invalidation failed: " / named("sessionId", sessionId)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.warning(e.nonFatalOrThrow()) { "Session expiration loop failed." }
                }
            }
        }
    }

    override suspend fun invalidate(id: String) {
        sessionLastActivityMap.remove(id)
        sessionStorageBackend.invalidate(id)
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
