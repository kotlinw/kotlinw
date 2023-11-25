package kotlinw.graph.algorithm

import kotlinw.graph.model.Graph
import kotlinw.graph.model.GraphRepresentation
import kotlinw.graph.model.Vertex
import kotlinw.util.stdlib.MutableBloomFilter
import kotlinw.util.stdlib.newMutableBloomFilter

fun <D : Any, V : Vertex<D>> Graph<D, V>.recursiveDfs(from: V): Sequence<Vertex<D>> {
    check(this is GraphRepresentation<D, V>)
    return recursiveDfsTraversal(from)
}

internal fun <D : Any, V : Vertex<D>> Graph<D, V>.recursiveDfsTraversal(
    from: V,
    onRevisitAttempt: (V) -> Unit = {}
): Sequence<V> {
    check(this is GraphRepresentation<D, V>)
    return sequence {
        visit(RecursiveDfsTraversalData(this@recursiveDfsTraversal, onRevisitAttempt), from)
    }
}

private data class RecursiveDfsTraversalData<D : Any, V : Vertex<D>>
private constructor(
    val graph: GraphRepresentation<D, V>,
    val visitedVerticesSet: MutableSet<V>,
    val visitedVerticesBloomFilter: MutableBloomFilter<V>,
    val onRevisitAttempt: (V) -> Unit
) {

    constructor(graph: GraphRepresentation<D, V>, onRevisitAttempt: (V) -> Unit) :
            this(
                graph,
                HashSet(graph.vertexCount),
                newMutableBloomFilter<V>(graph.vertexCount),
                onRevisitAttempt
            )
}

private suspend fun <D : Any, V : Vertex<D>> SequenceScope<V>.visit(
    traversalData: RecursiveDfsTraversalData<D, V>,
    vertex: V
) {
    if (traversalData.visitedVerticesBloomFilter.mightContain(vertex)
        && traversalData.visitedVerticesSet.contains(vertex)
    ) {
        traversalData.onRevisitAttempt(vertex)
    } else {
        traversalData.visitedVerticesSet.add(vertex)
        traversalData.visitedVerticesBloomFilter.add(vertex)

        yield(vertex)

        traversalData.graph.neighborsOf(vertex).forEach {
            visit(traversalData, it)
        }
    }
}
