package kotlinw.graph.algorithm

import arrow.core.raise.Raise
import arrow.core.raise.recover
import kotlinw.collection.ArrayStack
import kotlinw.graph.algorithm.AcyclicCheckResult.Acyclic
import kotlinw.graph.algorithm.AcyclicCheckResult.Cyclic
import kotlinw.graph.model.DirectedGraph
import kotlinw.graph.model.Vertex

sealed interface AcyclicCheckResult<D : Any, V : Vertex<D>> {

    class Acyclic<D : Any, V : Vertex<D>> : AcyclicCheckResult<D, V>

    data class Cyclic<D : Any, V : Vertex<D>>(val path: List<V>) : AcyclicCheckResult<D, V>
}

fun <D : Any, V : Vertex<D>> DirectedGraph<D, V>.isAcyclic() = checkAcyclic() is Acyclic<D, V>

fun <D : Any, V : Vertex<D>> DirectedGraph<D, V>.checkAcyclic() =
    recover({
        checkAcyclicImpl()
        Acyclic()
    }, {
        it
    })

context(Raise<Cyclic<D, V>>)
private fun <D : Any, V : Vertex<D>> DirectedGraph<D, V>.checkAcyclicImpl() {
    val visitedVertices = HashSet<V>()
    val recursionStack = ArrayStack<V>()
    vertices.forEach { startVertex ->
        if (startVertex !in visitedVertices) {
            visitedVertices.addAll(
                recursiveDfsTraversal(
                    from = startVertex,
                    onBeforeVertexVisited = { recursionStack.push(it) },
                    onAfterVertexVisited = { recursionStack.popOrNull() },
                    onVertexRevisited = {
                        if (it in recursionStack) {
                            raise(Cyclic(recursionStack.toList().asReversed()))
                        }
                    }
                )
            )
        }
    }
}
