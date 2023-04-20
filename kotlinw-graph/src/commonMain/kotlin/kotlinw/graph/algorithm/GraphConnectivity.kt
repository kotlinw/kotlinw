package kotlinw.graph.algorithm

import kotlinw.graph.model.Graph
import kotlinw.graph.model.GraphRepresentation
import kotlinw.graph.model.Vertex

fun <D : Any, V : Vertex<D>> Graph<D, V>.isConnected(): Boolean {
    check(this is GraphRepresentation<D, V>)

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
