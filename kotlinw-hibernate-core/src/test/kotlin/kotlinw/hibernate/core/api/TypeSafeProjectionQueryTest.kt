package kotlinw.hibernate.core.api

import jakarta.persistence.EntityManager
import kotlin.test.Test

class TypeSafeProjectionQueryTest {

    @Test
    fun testCompilationError() {
        val em = TypeSafeEntityManagerImpl(null as EntityManager)
        with(TypeSafeEntityManagerImpl(em)) {
            createProjectionQuery3<String, Int, String>("SELECT a, b, c FROM SomeEntity")
                .resultList
        }
    }
}
