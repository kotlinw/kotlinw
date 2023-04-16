package kotlinw.graph.model

fun <D : Any> DirectedGraph.Companion.build(builder: DirectedGraphBuilder<D, Vertex<D>>.() -> Unit): DirectedGraph<D, Vertex<D>> =
    DirectedGraphBuilderImpl<D>().let {
        builder(it)
        it.build()
    }

interface DirectedGraphBuilder<D : Any, V : Vertex<D>> {

    fun vertex(data: D): V

    fun edge(from: V, to: V)
}

private class DirectedGraphBuilderImpl<D : Any> : DirectedGraphBuilder<D, Vertex<D>> {

    private val vertices = LinkedHashMap<Vertex<D>, LinkedHashSet<Vertex<D>>>()

    override fun vertex(data: D): Vertex<D> {
        val vertex = VertexImpl(data)
        check(!vertices.containsKey(vertex)) { "Non-unique vertex data: $data" }
        vertices[vertex] = LinkedHashSet()
        return vertex
    }

    override fun edge(from: Vertex<D>, to: Vertex<D>) {
        check(vertices.containsKey(from)) { "Foreign 'from' vertex: $from" }
        check(vertices.containsKey(to)) { "Foreign 'to' vertex: $to" }

        vertices.getValue(from).add(to)
    }

    fun build(): DirectedGraph<D, Vertex<D>> = DirectedGraphAdjacencySetImpl(vertices)
}
