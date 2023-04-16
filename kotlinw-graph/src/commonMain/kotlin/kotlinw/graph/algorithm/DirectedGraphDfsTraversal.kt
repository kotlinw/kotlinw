package kotlinw.graph.algorithm

import kotlinw.graph.model.DirectedGraph
import kotlinw.graph.model.DirectedGraphRepresentation
import kotlinw.graph.model.Vertex
import kotlinw.util.stdlib.MutableBloomFilter
import kotlinw.util.stdlib.newMutableBloomFilter

fun <D: Any, V: Vertex<D>> DirectedGraph<D, V>.dfs(from: V): Sequence<Vertex<D>> {
    check(this is DirectedGraphRepresentation<D, V>)
    return dfsTraversal(from)
}

internal fun <D: Any, V: Vertex<D>> DirectedGraph<D, V>.dfsTraversal(
    from: V,
    onRevisitAttempt: (V) -> Unit = {}
): Sequence<V> {
    check(this is DirectedGraphRepresentation<D, V>)
    return sequence {
        visit(DfsTraversalData(this@dfsTraversal, onRevisitAttempt), from)
    }
}

private data class DfsTraversalData<D: Any, V: Vertex<D>> private constructor(
    val graph: DirectedGraphRepresentation<D, V>,
    val visitedVerticesSet: MutableSet<V>,
    val visitedVerticesBloomFilter: MutableBloomFilter<V>,
    val onRevisitAttempt: (V) -> Unit
) {

    constructor(graph: DirectedGraphRepresentation<D, V>, onRevisitAttempt: (V) -> Unit) :
            this(
                graph,
                HashSet(graph.vertexCount),
                newMutableBloomFilter<V>(graph.vertexCount * 4),
                onRevisitAttempt
            )
}

private suspend fun <D: Any, V: Vertex<D>> SequenceScope<V>.visit(
    traversalData: DfsTraversalData<D, V>, // TODO context(DfsTraversalData<V>)
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

        traversalData.graph.inNeighbors(vertex).forEach {
            visit(traversalData, it)
        }
    }
}
