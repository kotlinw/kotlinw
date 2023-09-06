package kotlinw.immutator.example.webapp

import kotlinw.immutator.annotation.Immutate

@Immutate
sealed interface TodoAppState {

    companion object

    val screenStack: List<Screen>
    val todoLists: List<TodoList>
}

val TodoAppState.mainScreen get() = screenStack.first() as MainScreen

val TodoAppStateMutable.mainScreen get() = screenStack.first() as MainScreenMutable

val TodoAppState.currentScreen get() = screenStack.last()

@Immutate
sealed interface Screen {

    companion object
}

@Immutate
sealed interface MainScreen : Screen {

    companion object

    val isCompletedShown: Boolean
}

@Immutate
sealed interface EditScreen : Screen {

    companion object

    val editedTodoListId: Long
}

@Immutate
sealed interface TodoList {

    companion object

    val id: Long
    val name: String
    val items: List<TodoItem>
}

fun <E : TodoList> List<E>.findById(id: Long): E = first { it.id == id }

@Immutate
sealed interface TodoItem {

    companion object

    val text: String
    val isCompleted: Boolean
}

val TodoItem.isActive get() = !isCompleted
