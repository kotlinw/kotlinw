package xyz.kotlinw.io

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NormalizedPathTest {

    @Test
    fun testHierarchy() {
        assertFalse(AbsolutePath("/var").isAncestorOf(AbsolutePath("/var")))
        assertFalse(AbsolutePath("/var").isDescendantOf(AbsolutePath("/var")))

        assertTrue(AbsolutePath("/var").isAncestorOf(AbsolutePath("/var/log")))
        assertTrue(AbsolutePath("/var/log").isDescendantOf(AbsolutePath("/var")))

        assertFalse(AbsolutePath("/var/log").isAncestorOf(AbsolutePath("/unrelated")))
        assertFalse(AbsolutePath("/unrelated").isDescendantOf(AbsolutePath("/var/log")))
    }

    @Test
    fun testRelativeTo() {
        assertEquals(RelativePath("log"), AbsolutePath("/var/log").asRelativeTo(AbsolutePath("/var")))
    }
}
