@file:OptIn(ExperimentalComposeWebApi::class)

package kotlinw.immutator.example.webapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.renderComposable

fun main() {
    renderComposable(rootElementId = "root") {
        val state by stateFlow.collectAsState()
        Application(state)
    }
}

@Composable
fun Application(state: TodoAppStateImmutable) {
    val currentScreen = state.currentScreen
    val isEditScreenOpen = currentScreen is EditScreen

    MainScreen(state.todoLists, state.mainScreen.isCompletedShown, isEditScreenOpen)

    if (isEditScreenOpen) {
        EditScreen(
            editedTodoList = state.todoLists.findById((currentScreen as EditScreen).editedTodoListId),
            onClose = { mutateState { it.closeCurrentScreen() } }
        )
    }
}
