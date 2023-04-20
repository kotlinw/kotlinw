package kotlinw.graph.algorithm

import kotlinw.graph.model.DirectedGraph
import kotlinw.graph.model.build
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AcyclicGraphTest {

    @Test
    fun testAcyclicDirectedGraphWithTwoNodes() {
        DirectedGraph.build {
            val v1 = vertex("1")
            val v2 = vertex("2")
            edge(v1, v2)
        }.apply {
            assertTrue(isAcyclic())
        }
    }

    @Test
    fun testAcyclicDirectedGraphWithThreeNodes() {
        DirectedGraph.build {
            val v1 = vertex("1")
            val v2 = vertex("2")
            val v3 = vertex("3")
            edge(v1, v2)
            edge(v1, v3)
            edge(v2, v3)
        }.apply {
            assertTrue(isAcyclic())
        }
    }

    @Test
    fun testCyclicDirectedGraphWithTwoNodes() {
        DirectedGraph.build {
            val v1 = vertex("1")
            val v2 = vertex("2")
            edge(v1, v2)
            edge(v2, v1)
        }.apply {
            assertFalse(isAcyclic())
        }
    }

    @Test
    fun testCyclicDirectedGraphWithThreeNodes() {
        DirectedGraph.build {
            val v1 = vertex("1")
            val v2 = vertex("2")
            val v3 = vertex("3")
            edge(v1, v2)
            edge(v2, v3)
            edge(v3, v1)
        }.apply {
            assertFalse(isAcyclic())
        }
    }
}
