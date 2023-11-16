package xyz.kotlinw.pwa

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import xyz.kotlinw.io.AbsolutePath
import xyz.kotlinw.io.ClasspathLocation
import xyz.kotlinw.io.ClasspathResource
import xyz.kotlinw.io.ClasspathScannerImpl
import xyz.kotlinw.io.RelativePath
import xyz.kotlinw.pwa.core.ResourceWebResourceMapping

class ResourceWebResourceMappingTest {

    @Test
    fun testClasspathResource() {
        val o = ResourceWebResourceMapping(
            RelativePath("public/js/sw.js"),
            ClasspathResource(ClasspathScannerImpl(), ClasspathLocation.of("xyz/kotlinw/pwa/core/js/sw-noop.js"))
        )
        assertEquals(
            RelativePath("public/js/sw.js"),
            o.getResourceWebPath(
                ClasspathResource(
                    ClasspathScannerImpl(),
                    ClasspathLocation.of("xyz/kotlinw/pwa/core/js/sw-noop.js")
                )
            )
        )
        assertNull(
            o.getResourceWebPath(
                ClasspathResource(
                    ClasspathScannerImpl(),
                    ClasspathLocation.of("xyz/kotlinw/pwa/core/js/sw-does-not-exist.js")
                )
            )
        )
    }
}
