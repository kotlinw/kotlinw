package kotlinw.graph.algorithm

import kotlinw.collection.ArrayQueue
import kotlinw.collection.MutableQueue
import kotlinw.graph.model.DirectedGraph
import kotlinw.graph.model.DirectedGraphRepresentation
import kotlinw.graph.model.Vertex
import kotlinw.util.stdlib.MutableBloomFilter
import kotlinw.util.stdlib.newMutableBloomFilter

fun <D : Any, V : Vertex<D>> DirectedGraph<D, V>.bfs(from: V): Sequence<Vertex<D>> {
    check(this is DirectedGraphRepresentation<D, V>)
    return bfsTraversal(from)
}

internal fun <D : Any, V : Vertex<D>> DirectedGraph<D, V>.bfsTraversal(
    from: V,
    onRevisitAttempt: (V) -> Unit = {}
): Sequence<V> {
    check(this is DirectedGraphRepresentation<D, V>)
    return sequence {
        val graph = this@bfsTraversal
        val visitedVerticesSet: MutableSet<V> = HashSet(graph.vertexCount)
        val visitedVerticesBloomFilter: MutableBloomFilter<V> = newMutableBloomFilter(graph.vertexCount)
        val queue: MutableQueue<V> = ArrayQueue()

        suspend fun SequenceScope<V>.markVisited(vertex: V) {
            queue.enqueue(vertex)
            visitedVerticesSet.add(vertex)
            visitedVerticesBloomFilter.add(vertex)

            yield(vertex)
        }

        fun V.isVisited() = visitedVerticesBloomFilter.mightContain(this) && visitedVerticesSet.contains(this)

        markVisited(from)

        while (queue.isNotEmpty()) {
            val current = queue.dequeueOrNull()!!
            graph.inNeighbors(current).forEach {
                if (it.isVisited()) {
                    onRevisitAttempt(it)
                } else {
                    markVisited(it)
                }
            }
        }
    }
}
