package kotlinw.graph.algorithm

import kotlinw.graph.model.DirectedGraph
import kotlinw.graph.model.DirectedGraphRepresentation
import kotlinw.graph.model.Vertex
import kotlinw.util.stdlib.MutableBloomFilter
import kotlinw.util.stdlib.newMutableBloomFilter

fun <V> DirectedGraph<V>.dfs(from: Vertex<V>): Sequence<Vertex<V>> {
    this as DirectedGraphRepresentation<V>
    return dfsTraversal(from)
}

internal fun <V> DirectedGraph<V>.dfsTraversal(
    from: Vertex<V>,
    onRevisitAttempt: (Vertex<V>) -> Unit = {}
): Sequence<Vertex<V>> {
    this as DirectedGraphRepresentation<V>
    return sequence {
        visit(DfsTraversalData(this@dfsTraversal, onRevisitAttempt), from)
    }
}

private data class DfsTraversalData<V> private constructor(
    val graph: DirectedGraphRepresentation<V>,
    val visitedVerticesSet: MutableSet<Vertex<V>>,
    val visitedVerticesBloomFilter: MutableBloomFilter<Vertex<V>>,
    val onRevisitAttempt: (Vertex<V>) -> Unit
) {

    constructor(graph: DirectedGraphRepresentation<V>, onRevisitAttempt: (Vertex<V>) -> Unit) :
            this(
                graph,
                HashSet(graph.vertexCount),
                newMutableBloomFilter<Vertex<V>>(graph.vertexCount / 2),
                onRevisitAttempt
            )
}

private suspend fun <V> SequenceScope<Vertex<V>>.visit(
    traversalData: DfsTraversalData<V>, // TODO context(DfsTraversalData<V>)
    vertex: Vertex<V>
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
