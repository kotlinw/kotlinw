package kotlinw.graph.model

internal sealed interface DirectedGraphRepresentation<D : Any, V : Vertex<D>> :
    GraphRepresentation<D, V>,
    DirectedGraph<D, V>
