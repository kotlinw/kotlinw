package kotlinw.immutator.example.webapp

import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.Clock.System

fun TodoAppStateMutable.closeCurrentScreen() {
    screenStack.removeLast()
}

fun TodoAppStateMutable.addNewTodoList() {
    todoLists.add(TodoListImmutable(System.now().toEpochMilliseconds(), "", persistentListOf()).toMutable())
}

fun TodoAppStateMutable.toggleShowCompletedItems() {
    val mainScreen = mainScreen
    mainScreen.isCompletedShown = !mainScreen.isCompletedShown
}

fun TodoAppStateMutable.openEditPopup(todoListId: Long) {
    screenStack.add(EditScreenImmutable(todoListId).toMutable())
}
