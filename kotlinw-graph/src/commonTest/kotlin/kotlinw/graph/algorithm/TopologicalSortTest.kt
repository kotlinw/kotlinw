package kotlinw.graph.algorithm

import kotlinw.graph.model.DirectedGraph
import kotlinw.graph.model.build
import kotlin.test.Test
import kotlin.test.assertEquals

class TopologicalSortTest {

    @Test
    fun testTopologicalSortWithTwoNodes() {
        DirectedGraph.build {
            val v1 = vertex("1")
            val v2 = vertex("2")
            edge(v1, v2)
        }.apply {
            assertEquals(listOf("1", "2"), topologicalSort().map { it.data })
        }

        DirectedGraph.build {
            val v1 = vertex("2")
            val v2 = vertex("1")
            edge(v2, v1)
        }.apply {
            assertEquals(listOf("1", "2"), topologicalSort().map { it.data })
        }
    }

    /**
     * https://guides.codepath.com/compsci/Topological-Sort
     */
    @Test
    fun testTopologicalSortOnCodePathData() {
        DirectedGraph.build {
            val v0 = vertex(0)
            val v1 = vertex(1)
            val v2 = vertex(2)
            val v3 = vertex(3)
            val v4 = vertex(4)
            val v5 = vertex(5)
            val v6 = vertex(6)
            edge(v0, v1)
            edge(v0, v2)
            edge(v1, v5)
            edge(v1, v2)
            edge(v2, v3)
            edge(v5, v3)
            edge(v5, v4)
            edge(v6, v1)
            edge(v6, v5)
        }.apply {
            assertEquals(listOf(6, 0, 1, 2, 5, 4, 3), topologicalSort().map { it.data })
        }
    }

    /**
     * https://en.wikipedia.org/wiki/Topological_sorting
     */
    @Test
    fun testTopologicalSortOnWikipediaData() {
        DirectedGraph.build {
            val v2 = vertex(2)
            val v3 = vertex(3)
            val v5 = vertex(5)
            val v7 = vertex(7)
            val v8 = vertex(8)
            val v9 = vertex(9)
            val v10 = vertex(10)
            val v11 = vertex(11)
            edge(v5, v11)
            edge(v7, v11)
            edge(v7, v8)
            edge(v3, v8)
            edge(v3, v10)
            edge(v11, v2)
            edge(v11, v9)
            edge(v11, v10)
            edge(v8, v9)
        }.apply {
            assertEquals(listOf(7, 5, 11, 3, 10, 8, 9, 2), topologicalSort().map { it.data })
        }
    }
}
