package kotlinw.graph.algorithm

import kotlinw.graph.model.DirectedGraph
import kotlinw.graph.model.DirectedGraphRepresentation
import kotlinw.graph.model.Vertex
import kotlinw.util.stdlib.MutableBloomFilter
import kotlinw.util.stdlib.newMutableBloomFilter

fun <D : Any, V : Vertex<D>> DirectedGraph<D, V>.recursiveDfs(from: V): Sequence<Vertex<D>> {
    check(this is DirectedGraphRepresentation<D, V>)
    return recursiveDfsTraversal(from)
}

internal fun <D : Any, V : Vertex<D>> DirectedGraph<D, V>.recursiveDfsTraversal(
    from: V,
    onRevisitAttempt: (V) -> Unit = {}
): Sequence<V> {
    check(this is DirectedGraphRepresentation<D, V>)
    return sequence {
        visit(RecursiveDfsTraversalData(this@recursiveDfsTraversal, onRevisitAttempt), from)
    }
}

private data class RecursiveDfsTraversalData<D : Any, V : Vertex<D>> private constructor(
    val graph: DirectedGraphRepresentation<D, V>,
    val visitedVerticesSet: MutableSet<V>,
    val visitedVerticesBloomFilter: MutableBloomFilter<V>,
    val onRevisitAttempt: (V) -> Unit
) {

    constructor(graph: DirectedGraphRepresentation<D, V>, onRevisitAttempt: (V) -> Unit) :
            this(
                graph,
                HashSet(graph.vertexCount),
                newMutableBloomFilter<V>(graph.vertexCount),
                onRevisitAttempt
            )
}

private suspend fun <D : Any, V : Vertex<D>> SequenceScope<V>.visit(
    traversalData: RecursiveDfsTraversalData<D, V>, // TODO context(DfsTraversalData<V>)
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

        traversalData.graph.inNeighborsOf(vertex).forEach {
            visit(traversalData, it)
        }
    }
}
