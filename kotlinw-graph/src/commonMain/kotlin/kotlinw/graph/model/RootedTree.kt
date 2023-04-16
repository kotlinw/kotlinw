package kotlinw.graph.model

interface RootedTreeNode<D: Any>: Vertex<D> {

    override val data: D

    val parent: RootedTreeNode<D>?

    val children: List<RootedTreeNode<D>>?
}

sealed interface RootedTree<D: Any>: Tree<D, RootedTreeNode<D>> {

    companion object

    val root: RootedTreeNode<D>
}

fun <D : Any> Tree<D, Vertex<D>>.toRootedTree(from: Vertex<D>): RootedTree<D> =
    RootedTreeRepresentation(
        TODO(),
        TODO()
    )
