package kotlinw.graph.model

sealed interface Tree<D : Any, V : Vertex<D>> : Graph<D, V> {
}
