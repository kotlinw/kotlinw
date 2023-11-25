package kotlinw.graph.algorithm

import kotlinw.graph.model.DirectedGraph
import kotlinw.graph.model.Vertex

class CyclicGraphException(val vertex: Vertex<*>) : RuntimeException("Cyclic graph detected while processing vertex: $vertex")

fun <D : Any, V : Vertex<D>> DirectedGraph<D, V>.isAcyclic(from: V): Boolean =
    try {
        dfs(from, onRevisitAttempt = { throw CyclicGraphException(it) }).forEach {
            // Do nothing
        }
        true
    } catch (e: CyclicGraphException) {
        false
    }

fun <D : Any, V : Vertex<D>> DirectedGraph<D, V>.isAcyclic(): Boolean = vertices.all { isAcyclic(it) }
