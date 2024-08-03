@file:OptIn(ExperimentalCompilerApi::class)

package xyz.kotlinw.di.processor

import kotlin.test.Test
import kotlinw.ksp.testutil.assertCompilationSucceeded
import kotlinw.ksp.testutil.assertHasKspError
import kotlinw.ksp.testutil.checkCompilationResult
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.di.api.Component

class GenericComponentTest {

    @Test
    fun testGenericComponents() {
        checkCompilationResult(
            """
                import ${Module::class.qualifiedName}
                import ${Component::class.qualifiedName}
                
                interface Converter<T: Any> {
                
                    fun convert(value: String): T
                }
                
                @Module
                class ConverterModule {
                
                    @Component
                    fun stringConverter() = 
                        object: Converter<String> {
                        
                            override fun convert(value: String) = value
                        }
                
                    @Component
                    fun intConverter() = 
                        object: Converter<Int> {
                        
                            override fun convert(value: String) = value.toInt()
                        }
                }
            """.trimIndent(),
            listOf(DiSymbolProcessorProvider())
        ) {
            assertCompilationSucceeded()
        }
    }
}
