package xyz.kotlinw.io

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.io.buffered
import kotlinx.io.readString
import org.junit.jupiter.api.assertThrows

class ClasspathResourceTest {

    @Test
    fun testClasspathDirectory() {
        ClasspathResource(AbsolutePath("xyz/kotlinw/io/dir")).run {
            assertFalse(exists())
            assertNull(length())
            assertThrows<ResourceResolutionException> { open() }
        }
    }

    @Test
    fun testClasspathFile() {
        ClasspathResource(AbsolutePath("xyz/kotlinw/io/dir/a.txt")).run {
            assertTrue(exists())
            assertEquals(1, length())
            assertEquals("a", open().buffered().use { it.readString() })
        }
    }
}
