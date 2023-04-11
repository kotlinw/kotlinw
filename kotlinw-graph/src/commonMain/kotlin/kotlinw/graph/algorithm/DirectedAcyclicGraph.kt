package kotlinw.graph.algorithm

import kotlinw.graph.model.DirectedGraph
import kotlinw.graph.model.Vertex

fun <V> DirectedGraph<V>.isAcyclic(from: Vertex<V>): Boolean {
    dfsTraversal(
        from,
        { true },
        { return@dfsTraversal false }
    )

    return true
}
