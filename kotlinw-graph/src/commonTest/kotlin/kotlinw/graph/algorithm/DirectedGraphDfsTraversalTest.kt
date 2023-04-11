package kotlinw.graph.algorithm

import kotlinw.graph.model.DirectedGraph
import kotlinw.graph.model.build
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DirectedGraphDfsTraversalTest {

    @Test
    fun test1() {
        val graph = DirectedGraph.build {
            val v1 = vertex("1")
            val v2 = vertex("2")
            val v3 = vertex("3")
            val v4 = vertex("4")
            val v5 = vertex("5")
            val v6 = vertex("6")

            edge(v1, v2)
            edge(v1, v3)
            edge(v2, v4)
            edge(v4, v5)
            edge(v3, v6)
        }

        assertEquals(
            listOf("1", "2", "4", "5", "3", "6"),
            graph.dfs(graph.vertices.first()).toList().map { it.data }
        )

        assertTrue(graph.isAcyclic(graph.vertices.first()))
    }
}
