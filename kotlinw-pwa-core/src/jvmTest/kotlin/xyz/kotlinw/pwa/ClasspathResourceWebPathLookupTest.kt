package xyz.kotlinw.pwa

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import xyz.kotlinw.io.AbsolutePath
import xyz.kotlinw.io.ClassPathResource
import xyz.kotlinw.io.RelativePath

class ClasspathResourceWebPathLookupTest {

    @Test
    fun test() {
        val o = ClasspathResourceWebPathLookup(RelativePath("public/text-files"), AbsolutePath("xyz/kotlinw/pwa/files"))
        assertEquals(
            RelativePath("public/text-files/a.txt"),
            o.getResourceWebPath(ClassPathResource(AbsolutePath("xyz/kotlinw/pwa/files/a.txt")))
        )
        assertEquals(
            RelativePath("public/text-files/folder/b.txt"),
            o.getResourceWebPath(ClassPathResource(AbsolutePath("xyz/kotlinw/pwa/files/folder/b.txt")))
        )
        assertNull(o.getResourceWebPath(ClassPathResource(AbsolutePath("xyz/kotlinw/pwa/files/x.txt"))))
    }
}
