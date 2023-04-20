package kotlinw.graph.model

fun <D : Any> UndirectedGraph.Companion.build(builder: GraphBuilder<D>.() -> Unit): UndirectedGraph<D, Vertex<D>> =
    UndirectedGraphBuilderImpl<D>().let {
        builder(it)
        it.build()
    }

fun <D : Any> DirectedGraph.Companion.build(builder: GraphBuilder<D>.() -> Unit): DirectedGraph<D, Vertex<D>> =
    DirectedGraphBuilderImpl<D>().let {
        builder(it)
        it.build()
    }

sealed interface GraphBuilder<D : Any> {

    fun vertex(data: D): Vertex<D>

    fun edge(from: Vertex<D>, to: Vertex<D>)
}

private abstract class AbstractGraphBuilderImpl<D : Any, G : Graph<D, Vertex<D>>> : GraphBuilder<D> {

    protected val vertices = LinkedHashMap<Vertex<D>, LinkedHashSet<Vertex<D>>>()

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

    abstract fun build(): G
}

private class DirectedGraphBuilderImpl<D : Any> :
    AbstractGraphBuilderImpl<D, DirectedGraph<D, Vertex<D>>>() {

    override fun build(): DirectedGraph<D, Vertex<D>> = DirectedGraphAdjacencySetRepresentationImpl(vertices)
}

private class UndirectedGraphBuilderImpl<D : Any> :
    AbstractGraphBuilderImpl<D, UndirectedGraph<D, Vertex<D>>>() {

    override fun edge(from: Vertex<D>, to: Vertex<D>) {
        super.edge(from, to)

        vertices.getValue(to).add(from)
    }

    override fun build(): UndirectedGraph<D, Vertex<D>> = UndirectedGraphAdjacencySetRepresentationImpl(vertices)
}
