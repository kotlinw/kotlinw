package kotlinw.remoting.client.processor

import kotlinw.ksp.testutil.assertCompilationFailed
import kotlinw.ksp.testutil.assertCompilationSucceeded
import kotlinw.ksp.testutil.assertHasKspError
import kotlinw.ksp.testutil.checkCompilationResult
import xyz.kotlinw.remoting.api.SupportsRemoting
import kotlinw.remoting.processor.RemotingSymbolProcessorProvider
import kotlinx.serialization.Serializable
import kotlin.test.Test

class CompilationValidationsTest {

    @Test
    fun testInterfaceIsSupported() {
        checkCompilationResult(
            """
                import ${SupportsRemoting::class.qualifiedName}
                
                @SupportsRemoting
                interface RemoteService {
                    companion object;
                }
            """.trimIndent(),
            listOf(RemotingSymbolProcessorProvider())
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
            """.trimIndent(),
            listOf(RemotingSymbolProcessorProvider())
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
                    companion object;
                    
                    suspend fun returnsFlow(): Flow<String?>
                }
            """.trimIndent(),
            listOf(RemotingSymbolProcessorProvider())
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
                    companion object;

                    @Serializable
                    data class A(val a: String)
                    
                    suspend fun a(): A
                }
            """.trimIndent(),
            listOf(RemotingSymbolProcessorProvider())
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
                    companion object;

                    suspend fun a(): List<String>
                }
            """.trimIndent(),
            listOf(RemotingSymbolProcessorProvider())
        ) {
            assertCompilationSucceeded()
        }
    }

    @Test
    fun testStarProjection() {
        checkCompilationResult(
            """
                import ${SupportsRemoting::class.qualifiedName}
                
                @SupportsRemoting
                interface RemoteService {
                    companion object;

                    suspend fun a(): List<*>
                }
            """.trimIndent(),
            listOf(RemotingSymbolProcessorProvider())
        ) {
            assertCompilationFailed()
        }
    }

    @Test
    fun testContextReceiver() {
        checkCompilationResult(
            """
                import ${SupportsRemoting::class.qualifiedName}
                
                @SupportsRemoting
                interface ContextReceiverExample {
                
                    companion object;
                
                    context(arrow.core.raise.Raise<kotlin.collections.List<kotlin.String>>)
                    suspend fun method1()
                }
            """.trimIndent(),
            listOf(RemotingSymbolProcessorProvider())
        ) {
            assertCompilationSucceeded()
        }
    }
}
