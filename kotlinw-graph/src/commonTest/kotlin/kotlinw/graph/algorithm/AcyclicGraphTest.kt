package kotlinw.graph.algorithm

import kotlinw.graph.model.DirectedGraph
import kotlinw.graph.model.build
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinw.graph.algorithm.AcyclicCheckResult.Cyclic
import kotlinw.graph.model.Vertex

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
        lateinit var v1: Vertex<String>
        lateinit var v2: Vertex<String>
        lateinit var v3: Vertex<String>
        DirectedGraph.build {
            v1 = vertex("1")
            v2 = vertex("2")
            v3 = vertex("3")
            edge(v1, v2)
            edge(v2, v3)
            edge(v3, v1)
        }.apply {
            assertFalse(isAcyclic())
            assertEquals(Cyclic(listOf(v1, v2, v3)), checkAcyclic())
        }
    }

    @Test
    fun testCyclicDirectedGraphWith4Nodes() {
        DirectedGraph.build {
            val v1 = vertex("1")
            val v2 = vertex("2")
            val v3 = vertex("3")
            val v4 = vertex("4")
            edge(v1, v2)
            edge(v1, v3)
            edge(v2, v4)
            edge(v3, v4)
        }.apply {
            assertTrue(isAcyclic())
        }
    }
}
