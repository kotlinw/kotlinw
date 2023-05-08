package kotlinw.remoting.client.processor

import kotlinw.remoting.api.SupportsRemoting
import kotlinx.serialization.Serializable
import java.util.concurrent.Flow
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

    @Test
    fun testFlowReturnType() {
        checkCompilationResult(
            """
                import ${SupportsRemoting::class.qualifiedName}
                import kotlinx.coroutines.flow.Flow
                
                @SupportsRemoting
                interface RemoteService {
                
                    suspend fun returnsFlow(): Flow<String?>
                }
            """.trimIndent()
        ) {
            assertCompilationSucceeded()
        }
    }

    @Test
    fun testSerializableReturnType() {
        checkCompilationResult(
            """
                import ${SupportsRemoting::class.qualifiedName}
                import ${Serializable::class.qualifiedName}
                
                @SupportsRemoting
                interface RemoteService {
                    
                    @Serializable
                    data class A(val a: String)
                    
                    suspend fun a(): A
                }
            """.trimIndent()
        ) {
            assertCompilationSucceeded()
        }
    }

    @Test
    fun testValidListTypeReturnType() {
        checkCompilationResult(
            """
                import ${SupportsRemoting::class.qualifiedName}
                
                @SupportsRemoting
                interface RemoteService {
                    
                    suspend fun a(): List<String>
                }
            """.trimIndent()
        ) {
            assertCompilationSucceeded()
        }
    }
}
