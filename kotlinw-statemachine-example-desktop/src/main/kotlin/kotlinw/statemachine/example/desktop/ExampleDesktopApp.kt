package kotlinw.statemachine.example.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinw.statemachine.compose.produceDataFetchState
import kotlinw.statemachine.util.DataFetchStatus.DataAvailable
import kotlinw.statemachine.util.DataFetchStatus.DataFetchFailed
import kotlinw.statemachine.util.DataFetchStatus.DataFetchInProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock.System
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private fun initialModifiers() = Modifier // .recomposeHighlighter()

fun main() {
    application {
        Window(
            title = "Example",
            onCloseRequest = { exitApplication() }
        ) {
            MaterialTheme {
                Application()
            }
        }
    }
}

fun log(text: String) {
    println("${System.now()} - $text")
}

@Composable
fun Application() {
    Column(modifier = initialModifiers()) {
        var input by remember { mutableStateOf(1) }

        val dataFetchState by produceDataFetchState(input) {
            try {
                log("Started fetching data... input=$it")
                delay(5.seconds)

                if (Random.nextInt(3) == 2) {
                    log("Data fetch failed! input=$it")
                    throw RuntimeException()
                }

                val data = sqrt(it.toFloat())
                log("Data loaded. input=$it, data=$data")
                data
            } catch (e: CancellationException) {
                log("Coroutine cancelled for input=$it.")
                throw e
            }
        }

        Text(modifier = initialModifiers(), text = "Current input: $input")
        Button(
            modifier = initialModifiers(),
            onClick = { input++ }
        ) {
            Text("Change input (increment by 1)")
        }

        when (val state = dataFetchState) {
            is DataFetchInProgress<Int> -> {
                val duration by produceState(Duration.ZERO, input) {
                    var elapsedSeconds = 0
                    while (true) {
                        value = (elapsedSeconds++).seconds
                        delay(1.seconds)
                    }
                }
                Text(modifier = initialModifiers(), text = "Fetching data... ${duration.inWholeSeconds}s")
            }
            is DataAvailable<Int, out Float> -> Text(
                modifier = initialModifiers(),
                text = "Result loaded: sqrt($input) = ${state.resultData}"
            )
            is DataFetchFailed<Int> -> Text(
                modifier = initialModifiers(),
                text = "Failed to fetch data: ${state.errorMessage}"
            )
        }
        Button(
            modifier = initialModifiers(),
            onClick = {
                when (val state = dataFetchState) {
                    is DataFetchInProgress<Int> -> {
                        log("Cancel initiated by user.")
                        state.cancel()
                    }
                    is DataAvailable<Int, out Float> -> {
                        log("Reload initiated by user.")
                        state.reload()
                    }
                    is DataFetchFailed<Int> -> {
                        log("Retry initiated by user.")
                        state.reload()
                    }
                }
            }
        ) {
            Text(
                modifier = initialModifiers(),
                text = when (dataFetchState) {
                    is DataFetchInProgress<Int> -> "Cancel"
                    is DataAvailable<Int, out Float> -> "Reload"
                    is DataFetchFailed<Int> -> "Retry"
                }
            )
        }
    }
}
