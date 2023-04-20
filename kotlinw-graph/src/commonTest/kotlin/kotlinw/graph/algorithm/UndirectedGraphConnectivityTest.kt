package kotlinw.graph.algorithm

import kotlinw.graph.model.UndirectedGraph
import kotlinw.graph.model.build
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UndirectedGraphConnectivityTest {

    @Test
    fun testOneVertexGraph() {
        UndirectedGraph.build {
            vertex("1")
        }.apply {
            assertTrue(isConnected())
        }
    }

    @Test
    fun testConnectedGraph() {
        UndirectedGraph.build {
            val v1 = vertex("1")
            val v2 = vertex("2")
            edge(v1, v2)
        }.apply {
            assertTrue(isConnected())
        }
    }

    @Test
    fun testDisconnectedGraph() {
        UndirectedGraph.build {
            vertex("1")
            vertex("2")
        }.apply {
            assertFalse(isConnected())
        }
    }
}
