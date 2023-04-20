package kotlinw.graph.model

sealed interface Graph<D : Any, V : Vertex<D>> {

    companion object

    val vertices: Sequence<V>

    fun neighborsOf(vertex: V): Sequence<V>
}
