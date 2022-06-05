package kotlinw.immutator.example.webapp

import kotlinw.immutator.annotation.Immutate

@Immutate
interface TodoAppState {
    val screenStack: List<Screen>
    val todoLists: List<TodoList>
}

val TodoAppState.mainScreen get() = screenStack.first() as MainScreen

val TodoAppStateMutable.mainScreen get() = screenStack.first() as MainScreenMutable

val TodoAppState.currentScreen get() = screenStack.last()

@Immutate
interface Screen

@Immutate
interface MainScreen : Screen {
    val isCompletedShown: Boolean
}

@Immutate
interface EditScreen : Screen {
    val editedTodoListId: Long
    val focusedItemIndex: Int?
}

@Immutate
interface TodoList {
    val id: Long
    val name: String
    val items: List<TodoItem>
}

fun <E : TodoList> List<E>.findById(id: Long): E = first { it.id == id }

@Immutate
interface TodoItem {
    val text: String
    val isCompleted: Boolean
}

val TodoItem.isActive get() = !isCompleted
