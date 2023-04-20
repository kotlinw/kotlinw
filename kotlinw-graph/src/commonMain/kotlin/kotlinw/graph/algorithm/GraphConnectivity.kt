package kotlinw.graph.algorithm

import kotlinw.graph.model.DirectedGraph
import kotlinw.graph.model.UndirectedGraph
import kotlinw.graph.model.Vertex
import kotlinw.graph.model.build
import kotlinw.graph.model.buildUnderlyingUndirectedGraph

fun <D : Any, V : Vertex<D>> UndirectedGraph<D, V>.isConnected(): Boolean {
    val vertices = this.vertices.toHashSet()

    if (vertices.size == 1) {
        return true
    }

    vertices.forEach { currentVertex ->
        if (dfs(currentVertex).toSet() != vertices) {
            return false
        }
    }

    return true
}

fun <D : Any, V : Vertex<D>> DirectedGraph<D, V>.isConnected(): Boolean =
    buildUnderlyingUndirectedGraph().isConnected()
