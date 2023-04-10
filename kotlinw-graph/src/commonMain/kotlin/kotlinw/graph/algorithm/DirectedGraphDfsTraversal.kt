package kotlinw.graph.algorithm

import kotlinw.graph.model.DirectedGraph
import kotlinw.graph.model.DirectedGraphRepresentation
import kotlinw.graph.model.Vertex

interface DirectedGraphDfsTraversal<V> {

    fun traverse(): Sequence<Vertex<V>>
}

fun <V> DirectedGraph<V>.dfs(from: Vertex<V> = vertices.first()): Sequence<Vertex<V>> {
    this as DirectedGraphRepresentation<V>
    return sequence {
        val visitedNodes = HashSet<V>(vertexCount)

        suspend fun SequenceScope<Vertex<V>>.visit(vertex: Vertex<V>) {
            if (!visitedNodes.contains(vertex.data)) {
                visitedNodes.add(vertex.data)
                yield(vertex)

                inNeighbors(vertex).forEach {
                    visit(it)
                }
            }
        }

        visit(from)
    }
}
