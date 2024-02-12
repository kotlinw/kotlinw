package xyz.kotlinw.remoting.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProduceStateScope
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlin.time.Duration.Companion.seconds
import kotlinw.util.stdlib.infiniteLoop
import kotlinx.coroutines.delay
import xyz.kotlinw.remoting.api.PersistentRemotingClient
import xyz.kotlinw.remoting.api.PersistentRemotingConnection

@Composable
fun <T> PersistentRemotingClient.produceStateWithConnection(
    initialValue: T,
    disconnectedValueProvider: (() -> T)? = null,
    onAfterDisconnection: suspend () -> Unit = { delay(1.seconds) },
    producer: suspend ProduceStateScope<T>.(PersistentRemotingConnection) -> Unit
): State<T> =
    produceState(initialValue) {
        infiniteLoop {
            this@produceStateWithConnection.withConnection {
                producer(it)
            }

            if (disconnectedValueProvider != null) {
                value = disconnectedValueProvider()
            }

            onAfterDisconnection()
        }
    }
