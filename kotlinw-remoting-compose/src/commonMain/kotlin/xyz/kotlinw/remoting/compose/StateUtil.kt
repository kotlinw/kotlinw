package xyz.kotlinw.remoting.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProduceStateScope
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinw.util.stdlib.infiniteLoop
import xyz.kotlinw.remoting.api.PersistentRemotingClient
import xyz.kotlinw.remoting.api.PersistentRemotingConnection

@Composable
fun <T> PersistentRemotingClient.produceStateWithConnection(
    initialValue: T,
    disconnectedValueProvider: () -> T = { initialValue },
    producer: suspend ProduceStateScope<T>.(PersistentRemotingConnection) -> Unit
): State<T> =
    produceState(initialValue) {
        infiniteLoop {
            this@produceStateWithConnection.withConnection {
                producer(it)
            }
            value = disconnectedValueProvider()
        }
    }
