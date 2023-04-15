package kotlinw.graph.algorithm

import kotlinw.graph.model.DirectedGraph
import kotlinw.graph.model.Vertex

// TODO raise
private class CyclicGraphException() : RuntimeException()

fun <V> DirectedGraph<V>.isAcyclic(from: Vertex<V>): Boolean =
    try {
        dfsTraversal(from, onRevisitAttempt = { throw CyclicGraphException() })
        true
    } catch (e: CyclicGraphException) {
        false
    }
