package kotlinw.graph.model

sealed interface UndirectedGraph<D: Any, V: Vertex<D>>: Graph<D, V> {

    companion object
}
