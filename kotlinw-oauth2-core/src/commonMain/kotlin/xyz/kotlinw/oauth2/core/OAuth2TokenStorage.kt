package xyz.kotlinw.oauth2.core

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.atomicfu.update
import kotlinx.atomicfu.updateAndGet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import xyz.kotlinw.oauth2.core.MutableOAuth2TokenStorage.RemovalHandle

interface OAuth2TokenStorage {

    val tokens: OAuth2BearerTokens?
}

typealias OAuth2TokenStorageChangeListener = (OAuth2BearerTokens?) -> Unit

interface MutableOAuth2TokenStorage : OAuth2TokenStorage {

    fun updateAndGetTokens(block: (OAuth2BearerTokens?) -> OAuth2BearerTokens?): OAuth2BearerTokens?

    fun interface RemovalHandle {

        fun remove()
    }

    fun addTokenChangeListener(block: OAuth2TokenStorageChangeListener): RemovalHandle
}

class MutableOAuth2TokenStorageImpl : MutableOAuth2TokenStorage {

    private val tokensHolder = atomic<OAuth2BearerTokens?>(null)

    override val tokens: OAuth2BearerTokens? get() = tokensHolder.value

    private val changeListeners = atomic<PersistentList<OAuth2TokenStorageChangeListener>>(persistentListOf())

    private val setTokensLock = reentrantLock()

    override fun updateAndGetTokens(block: (OAuth2BearerTokens?) -> OAuth2BearerTokens?): OAuth2BearerTokens? =
        setTokensLock.withLock {
            tokensHolder.updateAndGet {
                block(it)
            }
        }

    override fun addTokenChangeListener(block: OAuth2TokenStorageChangeListener): RemovalHandle {
        changeListeners.update {
            it.add(block)
        }
        return RemovalHandle {
            changeListeners.update {
                it.remove(block)
            }
        }
    }
}
