package kotlinw.graph.model

internal sealed interface DirectedGraphRepresentation<V : Any>: DirectedGraph<V> {

    val vertexCount: Int

    fun inNeighbors(from: Vertex<V>): Sequence<Vertex<V>>
}
