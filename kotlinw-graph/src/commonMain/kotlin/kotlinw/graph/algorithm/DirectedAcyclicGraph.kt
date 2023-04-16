package kotlinw.graph.algorithm

import kotlinw.graph.model.DirectedGraph
import kotlinw.graph.model.Vertex

private class CyclicGraphException : RuntimeException()

fun <V: Any> DirectedGraph<V>.isAcyclic(from: Vertex<V>): Boolean =
    try {
        dfsTraversal(from, onRevisitAttempt = { throw CyclicGraphException() })
        true
    } catch (e: CyclicGraphException) {
        false
    }
