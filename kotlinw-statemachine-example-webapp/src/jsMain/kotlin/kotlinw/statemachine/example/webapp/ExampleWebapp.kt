package kotlinw.statemachine.example.webapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinw.statemachine.compose.produceDataFetchState
import kotlinw.statemachine.util.DataFetchStatus.DataAvailable
import kotlinw.statemachine.util.DataFetchStatus.DataFetchFailed
import kotlinw.statemachine.util.DataFetchStatus.DataFetchInProgress
import kotlinx.browser.document
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Clock.System
import kotlinx.dom.appendElement
import kotlinx.dom.appendText
import org.jetbrains.compose.web.css.em
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun main() {
    renderComposable(rootElementId = "root") {
        Application()
    }
}

fun log(text: String) {
    document.getElementById("log")!!.apply {
        appendElement("div") {
            appendText("${System.now()} - $text")
            appendElement("br") {}
        }
    }
}

@Composable
fun SpanText(text: String) {
    key(text) {
        Span {
            Text(text)
        }
    }
}

@Composable
fun Application() {
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

    Div {
        Div {
            SpanText("Current input: $input")
            Br()
            Button({
                onClick { input++ }
            }) {
                SpanText("Change input (increment by 1)")
            }
        }
        Div({ style { marginTop(1.em) } }) {
            when (val state = dataFetchState) {
                is DataFetchInProgress<Int> -> {
                    val duration by produceState(Duration.ZERO, input) {
                        var elapsedSeconds = 0
                        while (true) {
                            value = (elapsedSeconds++).seconds
                            delay(1.seconds)
                        }
                    }
                    SpanText("Fetching data...")
                    SpanText("${duration.inWholeSeconds}s")
                }
                is DataAvailable<Int, out Float> -> SpanText("Result loaded: sqrt($input) = ${state.resultData}")
                is DataFetchFailed<Int> -> SpanText("Failed to fetch data: ${state.errorMessage}")
            }
            Br()
            Button({
                onClick {
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
            }) {
                SpanText(
                    when (dataFetchState) {
                        is DataFetchInProgress<Int> -> "Cancel"
                        is DataAvailable<Int, out Float> -> "Reload"
                        is DataFetchFailed<Int> -> "Retry"
                    }
                )
            }
        }
    }
}
