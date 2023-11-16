package xyz.kotlinw.io

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlinx.coroutines.test.runTest

class ClasspathScannerTest {

    @Test
    fun testScanNonEmptyDirectory() = runTest {
        assertEquals(
            listOf("a.txt", "b.txt"),
            ClasspathScannerImpl().scanDirectory(ClasspathLocation.of("xyz/kotlinw/io/dir")) { it.name.value }.values.toList()
        )
        assertEquals(
            setOf("a.txt", "b.txt", "f.txt"),
            ClasspathScannerImpl().scanDirectory(ClasspathLocation.of("xyz/kotlinw/io")) { it.name.value }.values.toSet()
        )
    }

    @Test
    fun testScanEmptyDirectory() = runTest {
        assertEquals(
            listOf(),
            ClasspathScannerImpl().scanDirectory(ClasspathLocation.of("xyz/kotlinw/io/emptydir")) { it.name }.values.toList()
        )
    }

    @Test
    fun testScanResourceResultsInEmptyResult() = runTest {
        assertEquals(
            listOf(),
            ClasspathScannerImpl().scanDirectory(ClasspathLocation.of("xyz/kotlinw/io/f.txt")) { it.name }.values.toList()
        )
    }

    @Test
    fun testScanNonExistingFolderResultsInEmptyResult() = runTest {
        assertEquals(
            listOf(),
            ClasspathScannerImpl().scanDirectory(ClasspathLocation.of("xyz/kotlinw/io/non-existing-directory")) { it.name }.values.toList()
        )
    }

    @Test
    fun testReadLength() = runTest {
        ClasspathScannerImpl().scanResource(ClasspathLocation.of("xyz/kotlinw/io/f.txt")) {
            assertEquals(3, it.length())
        }

        ClasspathScannerImpl().scanDirectory(ClasspathLocation.of("xyz/kotlinw/io/f.txt")) {
            assertEquals(
                when (it.name.value) {
                    "a.txt" -> 1
                    "b.txt" -> 2
                    "f.txt" -> 3
                    else -> fail(it.toString())
                },
                it.length()
            )
        }
    }

    @Test
    fun testReadContents() = runTest {
        ClasspathScannerImpl().scanResource(ClasspathLocation.of("xyz/kotlinw/io/f.txt")) {
            assertEquals("fff", it.loadUtf8String())
        }

        ClasspathScannerImpl().scanDirectory(ClasspathLocation.of("xyz/kotlinw/io/f.txt")) {
            assertEquals(
                when (it.name.value) {
                    "a.txt" -> "a"
                    "b.txt" -> "bb"
                    "f.txt" -> "fff"
                    else -> fail(it.toString())
                },
                it.loadUtf8String()
            )
        }
    }
}
