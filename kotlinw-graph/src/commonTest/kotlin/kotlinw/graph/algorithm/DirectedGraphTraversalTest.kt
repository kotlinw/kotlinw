package kotlinw.graph.algorithm

import kotlinw.graph.model.DirectedGraph
import kotlinw.graph.model.UndirectedGraph
import kotlinw.graph.model.Vertex
import kotlinw.graph.model.build
import kotlin.test.Test
import kotlin.test.assertEquals

class DirectedGraphTraversalTest {

    @Test
    fun testBfs() {
        val graph = createGraph()
        assertEquals(
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"),
            graph.bfs(graph.vertices.first()).toList().map { it.data }
        )
    }

    @Test
    fun testRecursiveDfs() {
        val graph = createGraph()
        assertEquals(
            listOf("1", "2", "5", "9", "10", "6", "3", "4", "7", "11", "12", "8"),
            graph.recursiveDfs(graph.vertices.first()).toList().map { it.data }
        )

        DirectedGraph.build {
            val v1 = vertex("1")
            val v2 = vertex("2")
            edge(v1, v2)
        }.apply {
            assertEquals(listOf("1", "2"), recursiveDfs(graph.vertices.first()).toList().map { it.data })
        }

        DirectedGraph.build {
            val v1 = vertex("1")
            val v2 = vertex("2")
            edge(v2, v1)
        }.apply {
            assertEquals(listOf("2", "1"), recursiveDfs(graph.vertices.first()).toList().map { it.data })
        }
    }

    @Test
    fun testDfs() {
        val graph = createGraph()
        assertEquals(
            listOf("1", "2", "5", "9", "10", "6", "3", "4", "7", "11", "12", "8"),
            graph.dfs(graph.vertices.first()).toList().map { it.data }
        )
    }

    private fun createGraph() = DirectedGraph.build {
        val v1 = vertex("1")
        val v2 = vertex("2")
        val v3 = vertex("3")
        val v4 = vertex("4")
        val v5 = vertex("5")
        val v6 = vertex("6")
        val v7 = vertex("7")
        val v8 = vertex("8")
        val v9 = vertex("9")
        val v10 = vertex("10")
        val v11 = vertex("11")
        val v12 = vertex("12")

        edge(v1, v2)
        edge(v1, v3)
        edge(v1, v4)
        edge(v2, v5)
        edge(v2, v6)
        edge(v5, v9)
        edge(v5, v10)
        edge(v4, v7)
        edge(v4, v8)
        edge(v7, v11)
        edge(v7, v12)
    }


    @Test
    fun testUndirectedGraphDfsStackOverflowBug() {
        lateinit var v1: Vertex<Int>
        lateinit var v2: Vertex<Int>

        val graph = UndirectedGraph.build {
            v1 = vertex(1)
            v2 = vertex(2)
            edge(v1, v2)
        }

        assertEquals(
            listOf(1, 2),
            graph.dfs(v1).map { it.data }.toList()
        )

        assertEquals(
            listOf(2, 1),
            graph.dfs(v2).map { it.data }.toList()
        )
    }
}
