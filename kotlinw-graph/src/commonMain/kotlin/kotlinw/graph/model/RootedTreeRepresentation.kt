package kotlinw.graph.model

private data class RootedTreeNodeImpl<D : Any>(
    override val data: D,
    override val parent: RootedTreeNode<D>?,
    override val children: List<RootedTreeNode<D>>?
) : RootedTreeNode<D>

internal class RootedTreeRepresentation<D : Any>(
    rootNode: RootedTreeNode<D>,
    nodeCount: Int
) : GraphRepresentation<D, RootedTreeNode<D>>, RootedTree<D> {

    override val root = rootNode

    override val vertexCount = nodeCount

    override fun neighborsOf(from: RootedTreeNode<D>): Sequence<RootedTreeNode<D>> =
        from.children?.asSequence() ?: emptySequence()

    override val vertices: Sequence<RootedTreeNode<D>> get() = TODO()
}
