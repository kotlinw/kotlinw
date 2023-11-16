package xyz.kotlinw.pwa

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import xyz.kotlinw.io.AbsolutePath
import xyz.kotlinw.io.ClasspathLocation
import xyz.kotlinw.io.ClasspathResource
import xyz.kotlinw.io.ClasspathScanner
import xyz.kotlinw.io.ClasspathScannerImpl
import xyz.kotlinw.io.RelativePath
import xyz.kotlinw.pwa.core.ClasspathFolderWebResourceMapping

class ClasspathFolderWebResourceMappingTest {

    @Test
    fun test() {
        val o = ClasspathFolderWebResourceMapping(
            RelativePath("public/text-files"),
            ClasspathLocation.of("xyz/kotlinw/pwa/files")
        )
        assertEquals(
            RelativePath("public/text-files/a.txt"),
            o.getResourceWebPath(
                ClasspathResource(
                    ClasspathScannerImpl(),
                    ClasspathLocation.of("xyz/kotlinw/pwa/files/a.txt")
                )
            )
        )
        assertEquals(
            RelativePath("public/text-files/folder/b.txt"),
            o.getResourceWebPath(
                ClasspathResource(
                    ClasspathScannerImpl(),
                    ClasspathLocation.of("xyz/kotlinw/pwa/files/folder/b.txt")
                )
            )
        )
        assertNull(
            o.getResourceWebPath(
                ClasspathResource(
                    ClasspathScannerImpl(),
                    ClasspathLocation.of("xyz/kotlinw/pwa/files/x.txt")
                )
            )
        )
    }
}
