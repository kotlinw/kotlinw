package kotlinw.koin.core.impl

import io.mockk.Ordering
import io.mockk.mockk
import io.mockk.verify
import kotlinw.koin.core.api.coreModule
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.dsl.onClose
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class OverrideSemanticsTest {

    interface Service<T>

    class ServiceImpl<T>: Service<T>

    @Test
    fun testMultipleInstancesOfSameType() {
        val s1 = ServiceImpl<Int>()
        val s2 = ServiceImpl<String>()

        val testModule = module {
            includes(coreModule)
            single<Service<*>>(named("a")) { s1 }
            single<Service<*>>(named("b")) { s2 }
        }

        val application = startKoin {
            allowOverride(false)
            modules(testModule)
        }

        assertEquals(setOf(s1, s2), application.koin.getAll<Service<*>>().toSet())
    }
}
