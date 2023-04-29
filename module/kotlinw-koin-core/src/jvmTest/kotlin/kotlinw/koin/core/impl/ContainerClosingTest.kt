package kotlinw.koin.core.impl

import io.mockk.Ordering
import io.mockk.mockk
import io.mockk.verify
import kotlinw.koin.core.api.koinCoreModule
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.dsl.onClose
import kotlin.test.Test

class ContainerClosingTest {

    interface Service1 : AutoCloseable

    interface Service2 : AutoCloseable

    @Test
    fun testAutoClose() {
        val m1 = mockk<Service1>(relaxed = true)
        val m2 = mockk<Service2>(relaxed = true)

        val testModule = module {
            includes(koinCoreModule())

            single { m1 } onClose { m1.close() }
            single { m2 } onClose { m2.close() }
        }

        val application = startKoin {
            modules(testModule)
        }

        application.koin.get<Service2>()
        application.koin.get<Service1>()

        application.close()

        verify(ordering = Ordering.ORDERED) {
            m2.close()
            m1.close()
        }
    }
}
