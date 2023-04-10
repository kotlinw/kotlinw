package kotlinw.graph.model

import arrow.core.raise.Raise

internal sealed interface DirectedGraphRepresentation<V>: DirectedGraph<V> {

    val vertexCount: Int

    fun inNeighbors(from: Vertex<V>): Sequence<Vertex<V>>
}
