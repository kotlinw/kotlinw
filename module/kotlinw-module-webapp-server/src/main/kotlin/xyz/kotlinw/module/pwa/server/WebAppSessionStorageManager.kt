package xyz.kotlinw.module.pwa.server

import arrow.core.nonFatalOrThrow
import io.ktor.server.sessions.SessionStorage
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.util.stdlib.infiniteLoop
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import kotlinx.datetime.Clock
import kotlinx.datetime.Clock.System
import kotlinx.datetime.Instant
import net.openhft.hashing.LongHashFunction
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.OnConstruction
import xyz.kotlinw.module.ktor.server.KtorServerApplicationConfigurer
import xyz.kotlinw.module.logging.LoggingModule

interface WebAppSessionStorageManager : SessionStorage {

    val activeSessionIds: Set<String>
}

@Component
class WebAppSessionStorageManagerImpl(
    private val sessionStorageBackendProvider: SessionStorageBackendProvider
) : KtorServerApplicationConfigurer(), WebAppSessionStorageManager {

    private val logger = LoggingModule.loggerFactory.getLogger()

    private lateinit var sessionStorageBackend: SessionStorage

    private data class SessionMetadata(val lastActivityTimestamp: Instant, val sessionDataHash: Long, val lock: Mutex)

    private val sessionMetadataMapHolder = atomic(persistentMapOf<String, SessionMetadata>())

    private val sessionMetadataMap by sessionMetadataMapHolder

    private val sessionTimeout = 30.minutes // FIXME config

    override val activeSessionIds: Set<String> get() = sessionMetadataMap.keys

    @OnConstruction
    fun onConstruction() {
        sessionStorageBackend = sessionStorageBackendProvider.createSessionStorageBackend()
    }

    override fun Context.setup() {
        ktorServerCoroutineScope.launch {
            infiniteLoop {
                delay(1.seconds) // TODO configurable

                logger.trace { "Checking session expiration..." }
                try {
                    val iterator = sessionMetadataMap.iterator()
                    while (iterator.hasNext()) {
                        val (sessionId, sessionMetadata) = iterator.next()

                        val isFirstActiveReached = sessionMetadata.lock.withLock {
                            if (sessionMetadataMap.containsKey(sessionId)) {
                                if (sessionMetadata.lastActivityTimestamp < Clock.System.now() - sessionTimeout) {
                                    logger.debug {
                                        "Session invalidation (expired): " / named("sessionId", sessionId)
                                    }
                                    try {
                                        invalidateSession(sessionId)
                                    } catch (e: Exception) {
                                        logger.warning(e.nonFatalOrThrow()) {
                                            "Session invalidation failed: " /
                                                    named("sessionId", sessionId)
                                        }
                                    }
                                    false
                                } else {
                                    true
                                }
                            } else {
                                false
                            }
                        }

                        if (isFirstActiveReached) {
                            break
                        }

                        yield()
                    }
                } catch (e: Exception) {
                    logger.warning(e.nonFatalOrThrow()) { "Session expiration loop failed." }
                }
            }
        }
    }

    override suspend fun invalidate(id: String) {
        sessionMetadataMap[id]?.run {
            lock.withLock {
                if (sessionMetadataMap.containsKey(id)) {
                    logger.debug { "Session invalidation (explicit): " / named("sessionId", id) }
                    invalidateSession(id)
                }
            }
        }
    }

    private suspend fun invalidateSession(id: String) {
        try {
            sessionStorageBackend.invalidate(id)
        } finally {
            sessionMetadataMapHolder.update {
                it.remove(id)
            }
        }
    }

    override suspend fun read(id: String): String {
        val sessionMetadata = sessionMetadataMap[id]
        return if (sessionMetadata != null) {
            sessionMetadata.lock.withLock {
                if (sessionMetadataMap.containsKey(id)) {
                    val value = sessionStorageBackend.read(id)
                    logger.trace { "Read session: " / mapOf("sessionId" to id, "sessionValue" to value) }

                    sessionMetadataMapHolder.update {
                        it.put(id, it.getValue(id).copy(lastActivityTimestamp = Clock.System.now()))
                    }

                    value
                } else {
                    throw NoSuchElementException("Session $id not found")
                }
            }
        } else {
            val value = sessionStorageBackend.read(id)
            logger.debug {
                "Read session (initialize from backend): " / mapOf(
                    "sessionId" to id,
                    "sessionValue" to value
                )
            }

            updateSessionMetadata(id, value)

            value
        }
    }

    override suspend fun write(id: String, value: String) {
        if (logger.isTraceEnabled) {
            logger.trace { "Write session: " / mapOf("sessionId" to id, "sessionValue" to value) }
        } else {
            logger.debug { "Write session: " / named("sessionId", id) }
        }

        updateSessionMetadata(id, value)

        sessionMetadataMap[id]?.also {
            it.lock.withLock {
                if (sessionMetadataMap[id]?.sessionDataHash != value.hash()) {
                    sessionStorageBackend.write(id, value)
                }
            }
        }
    }

    private fun updateSessionMetadata(id: String, value: String) {
        sessionMetadataMapHolder.update {
            val previousData = it[id]
            if (previousData != null) {
                it.put(id, previousData.copy(lastActivityTimestamp = System.now()))
            } else {
                it.put(id, SessionMetadata(System.now(), value.hash(), Mutex()))
            }
        }
    }

    private fun String.hash() = LongHashFunction.xx3().hashChars(this)
}
