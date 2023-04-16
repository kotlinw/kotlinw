package kotlinw.graph.model

sealed interface Vertex<out D: Any> {

    val data: D
}
