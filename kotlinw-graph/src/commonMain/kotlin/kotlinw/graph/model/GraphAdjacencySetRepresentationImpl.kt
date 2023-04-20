package kotlinw.graph.model

internal sealed class GraphAdjacencySetRepresentationImpl<D : Any, V : Vertex<D>, G : Graph<D, V>>(
    private val vertexAdjacencySets: LinkedHashMap<V, LinkedHashSet<V>>
) : GraphRepresentation<D, V> {

    override val vertexCount: Int get() = vertexAdjacencySets.size

    override fun neighborsOf(vertex: V): Sequence<V> =
        vertexAdjacencySets[vertex]?.asSequence() ?: throw ForeignGraphVertexException()

    override val vertices get() = vertexAdjacencySets.keys.asSequence()
}

internal class DirectedGraphAdjacencySetRepresentationImpl<D : Any, V : Vertex<D>>(vertexAdjacencySets: LinkedHashMap<V, LinkedHashSet<V>>) :
    GraphAdjacencySetRepresentationImpl<D, V, DirectedGraph<D, V>>(vertexAdjacencySets), DirectedGraph<D, V>

internal class UndirectedGraphAdjacencySetRepresentationImpl<D : Any, V : Vertex<D>>(vertexAdjacencySets: LinkedHashMap<V, LinkedHashSet<V>>) :
    GraphAdjacencySetRepresentationImpl<D, V, UndirectedGraph<D, V>>(vertexAdjacencySets), UndirectedGraph<D, V>
