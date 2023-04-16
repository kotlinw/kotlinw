package kotlinw.graph.model

fun <V : Any> DirectedGraph.Companion.build(builder: DirectedGraphBuilder<V>.() -> Unit): DirectedGraph<V> =
    DirectedGraphBuilderImpl<V>().let {
        builder(it)
        it.build()
    }

interface DirectedGraphBuilder<V: Any> {

    fun vertex(data: V): Vertex<V>

    fun edge(from: Vertex<V>, to: Vertex<V>)
}

private class DirectedGraphBuilderImpl<V: Any> : DirectedGraphBuilder<V> {

    private val vertices = mutableMapOf<V, MutableSet<V>>()

    override fun vertex(data: V): Vertex<V> {
        check(!vertices.containsKey(data)) { "Non-unique vertex data: $data" }
        vertices[data] = mutableSetOf()
        return VertexImpl(data)
    }

    override fun edge(from: Vertex<V>, to: Vertex<V>) {
        val fromData = from.data
        check(vertices.containsKey(fromData)) { "Foreign 'from' vertex: $from" }

        val toData = to.data
        check(vertices.containsKey(toData)) { "Foreign 'to' vertex: $to" }

        vertices.getValue(fromData).add(toData)
    }

    fun build(): DirectedGraph<V> = DirectedGraphAdjacencySetImpl(vertices)
}
