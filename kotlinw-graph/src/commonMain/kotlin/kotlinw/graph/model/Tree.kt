package kotlinw.graph.model

sealed interface Tree<D : Any, V:Vertex<D>>: Graph<D, V> {
}

// TODO
//fun <D : Any, V:Vertex<D>> Tree<D, V>.vertexCount(from: V) {
//    val countChildren = DeepRecursiveFunction<V, Int> { currentNode ->
//        1 + (neicurrentNode.nei?.sumOf { callRecursive(it) } ?: 0)
//    }
//    countChildren(from)
//}
