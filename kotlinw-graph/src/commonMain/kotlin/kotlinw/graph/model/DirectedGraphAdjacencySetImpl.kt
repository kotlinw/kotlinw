package kotlinw.graph.model

internal class DirectedGraphAdjacencySetImpl<D : Any, V : Vertex<D>>(
    private val vertexAdjacencySets: LinkedHashMap<V, LinkedHashSet<V>>
) : DirectedGraphRepresentation<D, V> {

    override val vertexCount: Int get() = vertexAdjacencySets.size

    override fun neighborsOf(from: V): Sequence<V> =
        vertexAdjacencySets[from]?.asSequence() ?: throw ForeignGraphVertexException()

    override val vertices get() = vertexAdjacencySets.keys.asSequence()
}
