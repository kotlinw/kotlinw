package kotlinw.graph.model

internal sealed interface GraphRepresentation<D : Any, V : Vertex<D>> : Graph<D, V>
