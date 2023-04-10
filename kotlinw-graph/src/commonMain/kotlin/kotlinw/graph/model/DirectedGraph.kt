package kotlinw.graph.model

sealed interface DirectedGraph<V> {

    companion object

    val vertices: Sequence<Vertex<V>>
}
