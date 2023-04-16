package kotlinw.graph.model

internal class DirectedGraphAdjacencySetImpl<V: Any>(
    private val vertexAdjacencySets: Map<V, Set<V>>
) : DirectedGraphRepresentation<V> {

    override val vertexCount: Int get() = vertexAdjacencySets.size

    override fun inNeighbors(from: Vertex<V>): Sequence<Vertex<V>> =
        vertexAdjacencySets[from.data]?.asSequence()?.map { VertexImpl(it) } ?: throw ForeignGraphVertexException()


    override val vertices: Sequence<Vertex<V>>
        get() = vertexAdjacencySets.keys.asSequence().map { VertexImpl(it) }

}
