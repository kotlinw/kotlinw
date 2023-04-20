package kotlinw.graph.algorithm

import kotlinw.graph.model.DirectedGraph
import kotlinw.graph.model.Vertex

private class CyclicGraphException : RuntimeException()

fun <D : Any, V : Vertex<D>> DirectedGraph<D, V>.isAcyclic(from: V): Boolean =
    try {
        recursiveDfsTraversal(from, onRevisitAttempt = { throw CyclicGraphException() })
        true
    } catch (e: CyclicGraphException) {
        false
    }
