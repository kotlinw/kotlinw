package xyz.kotlinw.module.webapp.client

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.module.webapp.client.ProgressIndicatorManager.ProgressIndicatorScope
import xyz.kotlinw.module.webapp.client.ProgressIndicatorManager.ProgressIndicatorState
import xyz.kotlinw.module.webapp.client.ProgressIndicatorManager.ProgressIndicatorState.ActiveProgressIndicator
import xyz.kotlinw.module.webapp.client.ProgressIndicatorManager.ProgressIndicatorState.NoProgressIndicator

val defaultDelayBeforeShow = 1.seconds

interface ProgressIndicatorManager {

    sealed interface ProgressIndicatorState {

        data class ActiveProgressIndicator(val text: String, val delayBeforeShow: Duration) : ProgressIndicatorState

        data object NoProgressIndicator : ProgressIndicatorState
    }

    val progressIndicatorStateFlow: StateFlow<ProgressIndicatorState>

    fun showProgressIndicator(text: String? = null, delayBeforeShow: Duration = 3.seconds)

    fun hideProgressIndicator()

    interface ProgressIndicatorScope {

        suspend fun <T> withProgressIndicator(
            text: String? = null,
            delayBeforeShow: Duration = defaultDelayBeforeShow,
            block: suspend ProgressIndicatorScope.() -> T
        ): T
    }

    suspend fun <T> withProgressIndicator(
        text: String? = null,
        delayBeforeShow: Duration = defaultDelayBeforeShow,
        block: suspend ProgressIndicatorScope.() -> T
    ): T
}

@Component
class ProgressIndicatorManagerImpl() : ProgressIndicatorManager {

    private val _progressIndicatorStateFlow = MutableStateFlow<ProgressIndicatorState>(NoProgressIndicator)

    override val progressIndicatorStateFlow: StateFlow<ProgressIndicatorState> =
        _progressIndicatorStateFlow.asStateFlow()

    override fun showProgressIndicator(text: String?, delayBeforeShow: Duration) {
        _progressIndicatorStateFlow.value =
            ActiveProgressIndicator(text ?: "Please wait...", delayBeforeShow) // TODO i18n
    }

    override fun hideProgressIndicator() {
        _progressIndicatorStateFlow.value = NoProgressIndicator
    }

    override suspend fun <T> withProgressIndicator(
        text: String?,
        delayBeforeShow: Duration,
        block: suspend ProgressIndicatorScope.() -> T
    ): T {
        val previousState = progressIndicatorStateFlow.value
        return try {
            showProgressIndicator(text, delayBeforeShow)
            block(
                object : ProgressIndicatorScope {

                    override suspend fun <T> withProgressIndicator(
                        text: String?,
                        delayBeforeShow: Duration,
                        block: suspend ProgressIndicatorScope.() -> T
                    ): T =
                        this@ProgressIndicatorManagerImpl.withProgressIndicator(text, delayBeforeShow, block)
                }
            )
        } finally {
            _progressIndicatorStateFlow.value = previousState
        }
    }
}
