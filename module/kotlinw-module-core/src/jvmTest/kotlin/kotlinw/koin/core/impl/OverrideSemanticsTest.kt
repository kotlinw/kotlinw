package kotlinw.koin.core.impl

import io.mockk.Ordering
import io.mockk.mockk
import io.mockk.verify
import kotlinw.koin.core.api.coreModule
import org.koin.core.context.startKoin
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.dsl.onClose
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class OverrideSemanticsTest {

    interface Service

    class Service1: Service

    class Service2: Service

    @Test
    fun testMultipleInstancesOfSameType() {
        val s1 = Service1()
        val s2 = Service2()

        val testModule = module {
            includes(coreModule)
            single { s1 }.bind<Service>()
            single { s2 }.bind<Service>()
        }

        val application = startKoin {
            modules(testModule)
        }

        assertEquals(setOf(s1, s2), application.koin.getAll<Service>().toSet())
    }
}
