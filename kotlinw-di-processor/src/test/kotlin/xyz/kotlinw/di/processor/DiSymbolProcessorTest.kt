package xyz.kotlinw.di.processor

import kotlin.test.Test
import kotlinw.ksp.testutil.assertCompilationFailed
import kotlinw.ksp.testutil.assertCompilationSucceeded
import kotlinw.ksp.testutil.assertHasKspError
import kotlinw.ksp.testutil.checkCompilationResult
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.di.api.Service

class DiSymbolProcessorTest {

    @Test
    fun testModuleClassShouldBeNormalClass() {
        checkCompilationResult(
            """
                import ${Module::class.qualifiedName}
                
                @Module
                interface Module1
            """.trimIndent(),
            listOf(DiSymbolProcessorProvider())
        ) {
            assertHasKspError("Module class should be a normal 'class', 'interface' is not supported as module declaration.", "Test.kt:4")
        }
    }

    @Test
    fun testEmptyModule() {
        checkCompilationResult(
            """
                import ${Module::class.qualifiedName}
                
                @Module
                class Module1
            """.trimIndent(),
            listOf(DiSymbolProcessorProvider())
        ) {
            assertCompilationSucceeded()
        }
    }

    @Test
    fun testModuleWithInlineService() {
        checkCompilationResult(
            """
                import ${Module::class.qualifiedName}
                import ${Service::class.qualifiedName}
                
                interface Service1
                
                class Service1Impl: Service1
                
                @Module
                class Module1 {
                
                    @Service
                    fun service1(): Service1 = Service1Impl()
                }
            """.trimIndent(),
            listOf(DiSymbolProcessorProvider())
        ) {
            assertCompilationSucceeded()
        }
    }
}
