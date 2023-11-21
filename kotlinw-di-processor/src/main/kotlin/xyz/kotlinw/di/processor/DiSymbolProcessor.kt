package xyz.kotlinw.di.processor

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.ClassKind.ANNOTATION_CLASS
import com.google.devtools.ksp.symbol.ClassKind.CLASS
import com.google.devtools.ksp.symbol.ClassKind.ENUM_CLASS
import com.google.devtools.ksp.symbol.ClassKind.ENUM_ENTRY
import com.google.devtools.ksp.symbol.ClassKind.INTERFACE
import com.google.devtools.ksp.symbol.ClassKind.OBJECT
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.validate
import xyz.kotlinw.di.api.Module

class DiSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols =
            resolver.getSymbolsWithAnnotation(Module::class.qualifiedName!!)
                .filterIsInstance<KSClassDeclaration>()
                .toSet()

        val validSymbols = symbols.filter { it.validate() }.toSet()
        validSymbols
            .filter {
                validateModuleClass(it, logger)
            }.forEach {
                processModuleClass(it, codeGenerator)
            }

        return (symbols - validSymbols).toList()
    }

    private fun processModuleClass(moduleClassDeclaration: KSClassDeclaration, codeGenerator: CodeGenerator) {
        moduleClassDeclaration.getDeclaredFunctions()
    }

    private fun validateModuleClass(moduleClassDeclaration: KSClassDeclaration, kspLogger: KSPLogger): Boolean =
        if (moduleClassDeclaration.classKind == CLASS) {
            if (moduleClassDeclaration.superTypes.filterIsInstance<KSClassDeclaration>()
                    .filter { it.qualifiedName?.asString() != Any::class.qualifiedName }.toList().isEmpty()
            ) {
                true
            } else {
                kspLogger.error(
                    "Explicit supertypes are currently not supported for module declarations.",
                    moduleClassDeclaration
                )
                false
            }
        } else {
            kspLogger.error(
                "Module class should be a normal 'class', '${moduleClassDeclaration.classKind.toDisplayName()}' is not supported as module declaration.",
                moduleClassDeclaration
            )
            false
        }
}

private fun ClassKind.toDisplayName(): String =
    when (this) {
        INTERFACE -> "interface"
        CLASS -> "class"
        ENUM_CLASS -> "enum class"
        ENUM_ENTRY -> "enum entry"
        OBJECT -> "object"
        ANNOTATION_CLASS -> "annotation class"
    }
