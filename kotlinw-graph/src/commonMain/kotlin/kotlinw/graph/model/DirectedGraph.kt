package kotlinw.graph.model

sealed interface DirectedGraph<D : Any, V : Vertex<D>> : Graph<D, V> {

    companion object
}

fun <D : Any, V : Vertex<D>> DirectedGraph<D, V>.inNeighborsOf(from: V): Sequence<V> = neighborsOf(from)
