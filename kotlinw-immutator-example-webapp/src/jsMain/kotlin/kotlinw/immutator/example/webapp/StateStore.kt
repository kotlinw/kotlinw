package kotlinw.immutator.example.webapp

import kotlinw.immutator.util.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

val stateFlow = MutableStateFlow(createInitialState())

inline fun mutateState(mutator: (TodoAppStateMutable) -> Unit) = stateFlow.update { it.mutate(mutator) }

private fun createInitialState() = TodoAppStateImmutable(
    persistentListOf(MainScreenImmutable(false)),
    persistentListOf(
        TodoListImmutable(
            0,
            "Personal",
            persistentListOf(
                TodoItemImmutable("Grocery shopping", false),
                TodoItemImmutable("Dog walking", false),
                TodoItemImmutable("Cleaning", true)
            )
        ),
        TodoListImmutable(
            1,
            "Work",
            persistentListOf(
                TodoItemImmutable("Write email to John", false),
                TodoItemImmutable("Organize meetings", false),
                TodoItemImmutable("Clean laptop screen", true),
                TodoItemImmutable("Clean laptop keyboard", true)
            )
        ),
        TodoListImmutable(
            2,
            "Garden",
            persistentListOf(
                TodoItemImmutable("Fit the bird-feeder", false),
                TodoItemImmutable("Plant flowers", true)
            )
        )
    )
)
