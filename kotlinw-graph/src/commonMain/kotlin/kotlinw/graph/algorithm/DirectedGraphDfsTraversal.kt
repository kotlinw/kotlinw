package kotlinw.graph.algorithm

import kotlinw.graph.model.DirectedGraph
import kotlinw.graph.model.DirectedGraphRepresentation
import kotlinw.graph.model.Vertex
import kotlin.coroutines.intrinsics.createCoroutineUnintercepted

fun <V> DirectedGraph<V>.dfs(from: Vertex<V>): LinkedHashSet<Vertex<V>> {
    this as DirectedGraphRepresentation<V>
    return dfsTraversal(from)
}

internal fun <V> DirectedGraph<V>.dfsTraversal(
    from: Vertex<V>,
    onRevisitAttempt: (Vertex<V>) -> Boolean = { true },
    onVisitVertex: (Vertex<V>) -> Boolean = { true }
): LinkedHashSet<Vertex<V>> {
    this as DirectedGraphRepresentation<V>
    val visitedNodes = LinkedHashSet<Vertex<V>>(vertexCount)

    val visit = DeepRecursiveFunction { vertex ->
        if (visitedNodes.contains(vertex)) {
            if (!onRevisitAttempt(vertex)) {
                return@DeepRecursiveFunction
            }
        } else {
            visitedNodes.add(vertex)
            if (onVisitVertex(vertex)) {
                inNeighbors(vertex).forEach {
                    this.callRecursive(it)
                }
            }
        }
    }

    visit(from)

    return visitedNodes
}
