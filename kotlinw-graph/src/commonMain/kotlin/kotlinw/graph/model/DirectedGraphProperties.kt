package kotlinw.graph.model

import kotlinw.graph.algorithm.isAcyclic

val DirectedGraph<Any, Vertex<Any>>.isAcyclic
    get() =
        when (this) {
            is Tree<*, *> -> true
            else -> vertices.all { isAcyclic(it) }
        }
