package kotlinw.remoting.client.processor

import kotlin.test.Test
import kotlinw.ksp.testutil.assertCompilationFailed
import kotlinw.ksp.testutil.assertCompilationSucceeded
import kotlinw.ksp.testutil.assertHasKspError
import kotlinw.ksp.testutil.checkCompilationResult
import kotlinw.remoting.processor.RemotingSymbolProcessorProvider
import kotlinx.serialization.Serializable
import xyz.kotlinw.remoting.api.SupportsRemoting

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
                interface Example {
                
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

    @Test
    fun testContextReceiverNonQualifiedTypeName() {
        checkCompilationResult(
            """
                import ${SupportsRemoting::class.qualifiedName}
                
                @SupportsRemoting
                interface Example {
                
                    companion object;
                
                    context(Raise<List<String>>)
                    suspend fun method1()
                }
            """.trimIndent(),
            listOf(RemotingSymbolProcessorProvider())
        ) {
            assertCompilationFailed()
            assertHasKspError("Failed to resolve context receiver type: Raise<List<String>> (Currently all type names must be fully qualified in the context receivers.)")
        }
    }

    @Test
    fun testContextReceiverInvalidTypeName() {
        checkCompilationResult(
            """
                import ${SupportsRemoting::class.qualifiedName}
                
                @SupportsRemoting
                interface Example {
                
                    companion object;
                
                    context(some.unknown.Type)
                    suspend fun method1()
                }
            """.trimIndent(),
            listOf(RemotingSymbolProcessorProvider())
        ) {
            assertCompilationFailed()
            assertHasKspError("Failed to resolve context receiver type: some.unknown.Type (Unknown type: some.unknown.Type)")
        }
    }

    @Test
    fun testInvalidContextReceiverOnlyRaiseSupported() {
        checkCompilationResult(
            """
                import ${SupportsRemoting::class.qualifiedName}
                
                @SupportsRemoting
                interface Example {
                
                    companion object;
                
                    context(kotlin.String)
                    suspend fun method1()
                }
            """.trimIndent(),
            listOf(RemotingSymbolProcessorProvider())
        ) {
            assertCompilationFailed()
            assertHasKspError("Only `arrow.core.raise.Raise` is supported in context receiver.", "Test.kt:9")
        }
    }
}
