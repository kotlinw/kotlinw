package kotlinw.graph.model

sealed interface Vertex<V: Any> {

    val data: V
}
