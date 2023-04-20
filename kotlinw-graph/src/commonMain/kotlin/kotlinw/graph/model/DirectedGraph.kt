package kotlinw.graph.model

sealed interface DirectedGraph<D: Any, V: Vertex<D>>: Graph<D, V> {

    companion object

    fun inNeighborsOf(from: V): Sequence<V> = neighborsOf(from)
}
