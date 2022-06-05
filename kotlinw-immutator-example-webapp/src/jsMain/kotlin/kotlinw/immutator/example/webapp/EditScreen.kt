package kotlinw.immutator.example.webapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.StyleScope
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.border
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.bottom
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.em
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.left
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.right
import org.jetbrains.compose.web.css.top
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.CheckboxInput
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextInput

@Composable
fun EditScreen(
    editedTodoList: TodoList,
    onClose: () -> Unit
) {
    Div({ classes("overlay") })
    Div({
        classes("editPopup")
        onClick { onClose() }
    }) {
        Div({
            style {
                border(1.px, LineStyle.Solid, Color.black)
                borderRadius(1.em)
                padding(1.em)
                backgroundColor(Color.white)
            }
            onClick { it.stopPropagation() }
        }) {
            TodoListEditor(editedTodoList)
        }
    }
}

@Composable
fun TodoListEditor(todoList: TodoList) {
    val todoListId = todoList.id

    fun TodoAppStateMutable.getTodoList() = todoLists.findById(todoListId)

    fun TodoAppStateMutable.getTodoListItems() = getTodoList().items

    Div({
        style {
            marginBottom(1.em)
        }
    }) {
        TextInput(todoList.name) {
            style {
                fontSize(20.px)
            }
            placeholder("Enter TODO list name")
            onInput {
                val newName = it.target.value
                mutateState {
                    it.getTodoList().name = newName
                }
            }
        }
    }
    Div {
        todoList.items.forEachIndexed { itemIndex, todoItem ->
            key(itemIndex) {
                Div {
                    CheckboxInput(
                        checked = todoItem.isCompleted,
                        attrs = {
                            onClick {
                                mutateState {
                                    val item = it.getTodoListItems()[itemIndex]
                                    item.isCompleted = !item.isCompleted
                                }
                            }
                        }
                    )
                    TextInput(todoItem.text) {
                        placeholder("Enter TODO item")
                        onInput {
                            val newItemText = it.value
                            mutateState {
                                val item = it.getTodoListItems()[itemIndex]
                                item.text = newItemText
                            }

                        }
                    }
                    Span({
                        style {
                            cursor("pointer")
                        }
                        onClick {
                            mutateState {
                                it.getTodoListItems().removeAt(itemIndex)
                            }
                        }
                    }) {
                        Text("ⓧ")
                    }
                }
            }
        }
    }
    Div({
        style {
            marginTop(0.5.em)
        }
    }) {
        Button({
            onClick {
                mutateState {
                    it.getTodoListItems().add(TodoItemImmutable("", false).toMutable())
                }
            }
        }) {
            Text("Add item")
        }
    }
}
