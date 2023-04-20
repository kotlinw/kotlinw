package kotlinw.graph.algorithm

import kotlinw.collection.ArrayStack
import kotlinw.collection.LinkedQueue
import kotlinw.collection.MutableQueue
import kotlinw.collection.MutableStack
import kotlinw.graph.model.DirectedGraph
import kotlinw.graph.model.DirectedGraphRepresentation
import kotlinw.graph.model.Vertex
import kotlinw.util.stdlib.MutableBloomFilter
import kotlinw.util.stdlib.newMutableBloomFilter
import kotlin.jvm.JvmInline

fun <D : Any, V : Vertex<D>> DirectedGraph<D, V>.bfs(from: V): Sequence<Vertex<D>> {
    check(this is DirectedGraphRepresentation<D, V>)
    return traverse(from, BfsRemainingVerticesHolder(), { inNeighbors(it).asIterable() })
}

fun <D : Any, V : Vertex<D>> DirectedGraph<D, V>.dfs(from: V): Sequence<Vertex<D>> {
    check(this is DirectedGraphRepresentation<D, V>)
    return traverse(from, DfsRemainingVerticesHolder(), { inNeighbors(it).asIterable().reversed() })
}

private sealed interface RemainingVerticesHolder<D : Any, V : Vertex<D>> {

    fun isEmpty(): Boolean

    fun add(vertex: V)

    fun remove(): V
}

private fun <D : Any, V : Vertex<D>> RemainingVerticesHolder<D, V>.isNotEmpty() = !isEmpty()

@JvmInline
private value class BfsRemainingVerticesHolder<D : Any, V : Vertex<D>>(private val queue: MutableQueue<V> = LinkedQueue()) :
    RemainingVerticesHolder<D, V> {

    override fun isEmpty() = queue.isEmpty()

    override fun remove(): V = queue.dequeueOrNull()!!

    override fun add(vertex: V) = queue.enqueue(vertex)
}

@JvmInline
private value class DfsRemainingVerticesHolder<D : Any, V : Vertex<D>>(private val stack: MutableStack<V> = ArrayStack()) :
    RemainingVerticesHolder<D, V> {

    override fun isEmpty() = stack.isEmpty()

    override fun remove(): V = stack.popOrNull()!!

    override fun add(vertex: V) = stack.push(vertex)
}

private fun <D : Any, V : Vertex<D>> DirectedGraph<D, V>.traverse(
    from: V,
    remainingVerticesHolder: RemainingVerticesHolder<D, V>,
    inNeighbors: (V) -> Iterable<V>,
    onRevisitAttempt: (V) -> Unit = {}
): Sequence<V> {
    check(this is DirectedGraphRepresentation<D, V>)
    return sequence {
        val graph = this@traverse
        val visitedVerticesSet: MutableSet<V> = HashSet(graph.vertexCount)
        val visitedVerticesBloomFilter: MutableBloomFilter<V> = newMutableBloomFilter(graph.vertexCount)

        fun markAsVisited(vertex: V) {
            visitedVerticesSet.add(vertex)
            visitedVerticesBloomFilter.add(vertex)
        }

        fun V.isVisited() = visitedVerticesBloomFilter.mightContain(this) && visitedVerticesSet.contains(this)

        remainingVerticesHolder.add(from)

        while (remainingVerticesHolder.isNotEmpty()) {
            val current = remainingVerticesHolder.remove()

            if (current.isVisited()) {
                onRevisitAttempt(current)
            } else {
                markAsVisited(current)
                yield(current)

                inNeighbors(current).forEach {
                    if (!it.isVisited()) {
                        remainingVerticesHolder.add(it)
                    }
                }
            }
        }
    }
}
