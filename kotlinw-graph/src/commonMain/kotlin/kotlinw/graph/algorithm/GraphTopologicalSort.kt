package kotlinw.graph.algorithm

import kotlinw.graph.model.Graph
import kotlinw.graph.model.GraphRepresentation
import kotlinw.graph.model.Vertex
import kotlinw.util.stdlib.MutableBloomFilter
import kotlinw.util.stdlib.newMutableBloomFilter

fun <D : Any, V : Vertex<D>> Graph<D, V>.topologicalSort(): List<Vertex<D>> =
    reverseTopologicalSort().toList().asReversed()

fun <D : Any, V : Vertex<D>> Graph<D, V>.reverseTopologicalSort(): Sequence<Vertex<D>> {
    check(this is GraphRepresentation<D, V>)
    return sequence {
        val contextData = TopologicalSortData(this@reverseTopologicalSort)
        while (contextData.visitedVerticesSet.size < vertexCount) {
            visit(
                contextData,
                vertices.first {
                    !contextData.visitedVerticesBloomFilter.mightContain(it)
                            || !contextData.visitedVerticesSet.contains(it)
                }
            )
        }
    }
}

private data class TopologicalSortData<D : Any, V : Vertex<D>>
private constructor(
    val graph: GraphRepresentation<D, V>,
    val visitedVerticesSet: MutableSet<V>,
    val visitedVerticesBloomFilter: MutableBloomFilter<V>
) {

    constructor(graph: GraphRepresentation<D, V>) :
            this(
                graph,
                HashSet(graph.vertexCount),
                newMutableBloomFilter<V>(graph.vertexCount)
            )
}

private suspend fun <D : Any, V : Vertex<D>> SequenceScope<V>.visit(
    traversalData: TopologicalSortData<D, V>,
    vertex: V
) {
    if (!traversalData.visitedVerticesBloomFilter.mightContain(vertex)
        || !traversalData.visitedVerticesSet.contains(vertex)
    ) {
        traversalData.visitedVerticesSet.add(vertex)
        traversalData.visitedVerticesBloomFilter.add(vertex)

        traversalData.graph.neighborsOf(vertex).forEach {
            visit(traversalData, it)
        }

        yield(vertex)
    }
}
