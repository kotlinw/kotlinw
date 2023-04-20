package kotlinw.graph.algorithm

import kotlinw.graph.model.DirectedGraph
import kotlinw.graph.model.Vertex

private class CyclicGraphException : RuntimeException()

fun <D : Any, V : Vertex<D>> DirectedGraph<D, V>.isAcyclic(from: V): Boolean =
    try {
        recursiveDfsTraversal(from, onRevisitAttempt = { throw CyclicGraphException() }).forEach {
            // Do nothing
        }
        true
    } catch (e: CyclicGraphException) {
        false
    }

fun <D : Any, V : Vertex<D>> DirectedGraph<D, V>.isAcyclic(): Boolean = vertices.all { isAcyclic(it) }
