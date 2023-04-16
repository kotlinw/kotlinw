package kotlinw.graph.model

sealed interface DirectedGraph<V: Any> {

    companion object

    val vertices: Sequence<Vertex<V>>
}
