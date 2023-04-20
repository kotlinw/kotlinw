package kotlinw.graph.model

sealed interface DirectedGraph<D : Any, V : Vertex<D>> : Graph<D, V> {

    companion object
}

fun <D : Any, V : Vertex<D>> DirectedGraph<D, V>.inNeighborsOf(from: V): Sequence<V> = neighborsOf(from)

fun <D : Any, V : Vertex<D>> DirectedGraph<D, V>.buildUnderlyingUndirectedGraph() =
    UndirectedGraph.build {
        vertices.forEach {
            vertex(it.data)
        }

        vertices.forEach { current ->
            neighborsOf(current).forEach { neighbour ->
                edge(current, neighbour)
            }
        }
    }
