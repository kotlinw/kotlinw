package kotlinw.remoting.client.processor

import kotlinw.remoting.api.SupportsRemoting
import kotlin.test.Test

class CompilationValidationsTest {

    @Test
    fun testInterfaceIsSupported() {
        checkCompilationResult(
            """
                import ${SupportsRemoting::class.qualifiedName}
                
                @SupportsRemoting
                interface RemoteService
            """.trimIndent()
        ) {
            assertCompilationSucceeded()
        }
    }

    @Test
    fun testOnlyInterfaceIsSupported() {
        checkCompilationResult(
            """
                import ${SupportsRemoting::class.qualifiedName}
                
                @SupportsRemoting
                class RemoteService
            """.trimIndent()
        ) {
            assertHasKspError("Only interface declarations should be annotated with @SupportsRemoting.", "Test.kt:4")
        }
    }
}