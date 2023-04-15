package kotlinw.graph.algorithm

import kotlinw.graph.model.DirectedGraph
import kotlinw.graph.model.DirectedGraphRepresentation
import kotlinw.graph.model.Vertex

fun <V> DirectedGraph<V>.dfs(from: Vertex<V>): Sequence<Vertex<V>> {
    this as DirectedGraphRepresentation<V>
    return dfsTraversal(from)
}

internal fun <V> DirectedGraph<V>.dfsTraversal(
    from: Vertex<V>,
    onRevisitAttempt: (Vertex<V>) -> Unit = {}
): Sequence<Vertex<V>> {
    this as DirectedGraphRepresentation<V>
    return sequence {
        visit(this@dfsTraversal, from, HashSet(vertexCount), onRevisitAttempt)
    }
}

internal suspend fun <V> SequenceScope<Vertex<V>>.visit(
    graph: DirectedGraphRepresentation<V>,
    vertex: Vertex<V>,
    visitedNodes: HashSet<Vertex<V>>, // TODO more efficient data structure
    onRevisitAttempt: (Vertex<V>) -> Unit
) {
    if (visitedNodes.contains(vertex)) {
        onRevisitAttempt(vertex)
    } else {
        visitedNodes.add(vertex)
        yield(vertex)

        graph.inNeighbors(vertex).forEach {
            visit(graph, it, visitedNodes, onRevisitAttempt)
        }
    }
}
