package xyz.kotlinw.module.webapp.client.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import xyz.kotlinw.module.webapp.client.ProgressIndicatorManager

context(ProgressIndicatorManager)
@Composable
fun <T: Any> DeferUntilDataLoaded(
    loadData: suspend () -> T,
    content: @Composable (T) -> Unit
) {
    var loadedData by remember { mutableStateOf<T?>(null) }

    LaunchedEffect(true) {
        withProgressIndicator {
            loadedData = loadData()
        }
    }

    if (loadedData != null) {
        content(loadedData!!)
    }
}
