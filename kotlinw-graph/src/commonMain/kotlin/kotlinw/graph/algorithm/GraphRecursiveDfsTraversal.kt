package kotlinw.graph.algorithm

import kotlinw.graph.model.Graph
import kotlinw.graph.model.GraphRepresentation
import kotlinw.graph.model.Vertex

fun <D : Any, V : Vertex<D>> Graph<D, V>.recursiveDfs(): Sequence<Vertex<D>> =
    sequence {
        val visitedVertices = HashSet<Vertex<D>>()
        vertices.forEach { startVertex ->
            if (startVertex !in visitedVertices) {
                recursiveDfs(startVertex).forEach { visitedVertex ->
                    visitedVertices.add(visitedVertex)
                    yield(visitedVertex)
                }
            }
        }
    }

fun <D : Any, V : Vertex<D>> Graph<D, V>.recursiveDfs(
    from: V
): Sequence<Vertex<D>> {
    check(this is GraphRepresentation<D, V>)
    return recursiveDfsTraversal(from, {}, {}, {})
}

internal fun <D : Any, V : Vertex<D>> Graph<D, V>.recursiveDfsTraversal(
    from: V,
    onBeforeVertexVisited: (V) -> Unit,
    onAfterVertexVisited: (V) -> Unit,
    onVertexRevisited: (V) -> Unit
): Sequence<V> {
    check(this is GraphRepresentation<D, V>)
    return sequence {
        visit(
            RecursiveDfsTraversalData(
                this@recursiveDfsTraversal, onBeforeVertexVisited, onAfterVertexVisited, onVertexRevisited
            ),
            from
        )
    }
}

private data class RecursiveDfsTraversalData<D : Any, V : Vertex<D>>
private constructor(
    val graph: GraphRepresentation<D, V>,
    val onBeforeVertexVisited: (V) -> Unit,
    val onAfterVertexVisited: (V) -> Unit,
    val onVertexRevisited: (V) -> Unit,
    val visitedVerticesSet: MutableSet<V>
) {

    constructor(
        graph: GraphRepresentation<D, V>,
        onBeforeVertexVisited: (V) -> Unit,
        onAfterVertexVisited: (V) -> Unit,
        onVertexRevisited: (V) -> Unit
    ) : this(graph, onBeforeVertexVisited, onAfterVertexVisited, onVertexRevisited, HashSet(graph.vertexCount))
}

private suspend fun <D : Any, V : Vertex<D>> SequenceScope<V>.visit(
    traversalData: RecursiveDfsTraversalData<D, V>,
    vertex: V
) {
    if (!traversalData.visitedVerticesSet.contains(vertex)) {
        traversalData.onBeforeVertexVisited(vertex)

        traversalData.visitedVerticesSet.add(vertex)
        yield(vertex)

        traversalData.graph.neighborsOf(vertex).forEach {
            visit(traversalData, it)
        }

        traversalData.onAfterVertexVisited(vertex)
    } else {
        traversalData.onVertexRevisited(vertex)
    }
}
