package kotlinw.graph.algorithm

import kotlinw.collection.ArrayStack
import kotlinw.collection.LinkedQueue
import kotlinw.collection.MutableQueue
import kotlinw.collection.MutableStack
import kotlinw.graph.model.Graph
import kotlinw.graph.model.Vertex
import kotlinw.util.stdlib.MutableBloomFilter
import kotlinw.util.stdlib.newMutableBloomFilter
import kotlin.jvm.JvmInline

fun <D : Any, V : Vertex<D>> Graph<D, V>.bfs(from: V): Sequence<Vertex<D>> {
    return traverse(from, BfsRemainingVerticesHolder(), { neighborsOf(it) })
}

fun <D : Any, V : Vertex<D>> Graph<D, V>.dfs(from: V): Sequence<Vertex<D>> {
    return traverse(from, DfsRemainingVerticesHolder(), { neighborsOf(it) })
}

private sealed interface RemainingVerticesHolder<D : Any, V : Vertex<D>> {

    fun isNotEmpty(): Boolean

    fun add(vertex: V)

    fun remove(): V
}

@JvmInline
private value class BfsRemainingVerticesHolder<D : Any, V : Vertex<D>>(private val queue: MutableQueue<V> = LinkedQueue()) :
    RemainingVerticesHolder<D, V> {

    override fun isNotEmpty() = queue.size > 0

    override fun remove(): V = queue.dequeueOrNull()!!

    override fun add(vertex: V) = queue.enqueue(vertex)
}

@JvmInline
private value class DfsRemainingVerticesHolder<D : Any, V : Vertex<D>>(private val stack: MutableStack<V> = ArrayStack()) :
    RemainingVerticesHolder<D, V> {

    override fun isNotEmpty() = stack.size > 0

    override fun remove(): V = stack.popOrNull()!!

    override fun add(vertex: V) = stack.push(vertex)
}

private fun <D : Any, V : Vertex<D>> Graph<D, V>.traverse(
    from: V,
    remainingVerticesHolder: RemainingVerticesHolder<D, V>,
    outNeighbors: (V) -> Sequence<V>
): Sequence<V> {
    return sequence {
        val graph = this@traverse
        val visitedVerticesSet: MutableSet<V> = HashSet(graph.vertexCount)
        val visitedVerticesBloomFilter: MutableBloomFilter<V> = newMutableBloomFilter(graph.vertexCount)

        fun V.markAsVisited() {
            visitedVerticesSet.add(this)
            visitedVerticesBloomFilter.add(this)
        }

        fun V.isVisited() = visitedVerticesBloomFilter.mightContain(this) && visitedVerticesSet.contains(this)

        remainingVerticesHolder.add(from)

        while (remainingVerticesHolder.isNotEmpty()) {
            val current = remainingVerticesHolder.remove()
            current.markAsVisited()
            yield(current)

            outNeighbors(current).forEach {
                if (!it.isVisited()) {
                    remainingVerticesHolder.add(it)
                }
            }
        }
    }
}
