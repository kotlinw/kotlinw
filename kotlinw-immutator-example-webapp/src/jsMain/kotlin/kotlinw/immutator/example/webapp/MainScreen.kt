@file:OptIn(ExperimentalComposeWebApi::class, ExperimentalComposeWebApi::class)

package kotlinw.immutator.example.webapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.css.filter
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.CheckboxInput
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.Text

@Composable
fun MainScreen(todoLists: ImmutableList<TodoListImmutable>, isCompletedShown: Boolean, blurContent: Boolean) {
    Div({
        if (blurContent) {
            style {
                filter {
                    blur(2.px)
                }
            }
        }
    }) {
        Div {
            CheckboxInput(
                checked = isCompletedShown,
                attrs = {
                    onClick {
                        mutateState { it.toggleShowCompletedItems() }
                    }
                }
            )
            Text("Show completed items")
        }
        Div({ classes("container") }) {
            todoLists.forEach { todoList ->
                key(todoList) {
                    Div({
                        classes("item")
                        onClick {
                            mutateState { it.openEditPopup(todoList.id) }
                        }
                    }) {
                        TodoListView(todoList, isCompletedShown)
                    }
                }
            }
        }
        Div {
            Button({
                onClick { mutateState { it.addNewTodoList() } }
            }) {
                Text("Add new TODO list")
            }
        }
    }
}

@Composable
fun TodoListView(todoList: TodoListImmutable, isCompletedShown: Boolean) {
    Div {
        H3 {
            Text(todoList.name)
        }
        Div {
            val items by derivedStateOf { todoList.items.filter { if (isCompletedShown) true else it.isActive } }
            items.forEach {
                key(it) {
                    Div {
                        if (isCompletedShown) {
                            if (it.isCompleted) Text("☑ ") else Text("☐ ")
                        } else {
                            Text("• ")
                        }
                        Text(it.text)
                    }
                }
            }
        }
    }
}
