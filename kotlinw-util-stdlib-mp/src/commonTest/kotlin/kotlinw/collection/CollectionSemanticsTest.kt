package kotlinw.collection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.collections.immutable.persistentMapOf

class CollectionSemanticsTest {

    @Test
    fun testPersistentMapPut() {
        var m = persistentMapOf<String, Unit>()
        m = m.put("a", Unit)
        m = m.put("b", Unit)
        m = m.put("c", Unit)
        assertEquals(setOf("a", "b", "c").associateWith { Unit }, m)
        m = m.put("a", Unit)
        assertEquals(setOf("b", "c", "a").associateWith { Unit }, m)
    }
}
