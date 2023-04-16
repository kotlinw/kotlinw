package kotlinw.graph.model

internal sealed interface GraphRepresentation<D : Any, V : Vertex<D>> : Graph<D, V> {

    val vertexCount: Int

    fun neighborsOf(from: V): Sequence<V>
}
