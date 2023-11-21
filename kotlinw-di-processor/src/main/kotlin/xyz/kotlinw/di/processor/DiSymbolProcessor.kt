package xyz.kotlinw.di.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.isAnnotationPresent
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
import com.google.devtools.ksp.validate
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.di.api.Service

class DiSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val kspLogger: KSPLogger,
    private val kspOptions: Map<String, String>
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols =
            resolver.getSymbolsWithAnnotation(Module::class.qualifiedName!!)
                .filterIsInstance<KSClassDeclaration>()
                .toSet()

        val validSymbols = symbols.filter { it.validate() }.toSet()
        validSymbols
            .filter {
                validateModuleClass(it, resolver)
            }.forEach {
                processModuleClass(it, resolver)
            }

        return (symbols - validSymbols).toList()
    }

    @OptIn(KspExperimental::class)
    private fun processModuleClass(
        moduleClassDeclaration: KSClassDeclaration,
        resolver: Resolver
    ) {
        moduleClassDeclaration.getDeclaredFunctions()
            .filter { it.isAnnotationPresent(Service::class) }
            .forEach {
                kspLogger.warn("xxx: " + it.simpleName.asString())
            }
    }

    private fun validateModuleClass(moduleClassDeclaration: KSClassDeclaration, resolver: Resolver): Boolean =
        if (moduleClassDeclaration.classKind == CLASS) {
            if (moduleClassDeclaration.superTypes
                    .filter { it.resolve() != resolver.builtIns.anyType }
                    .toList()
                    .isEmpty()
            ) {
                true
            } else {
                kspLogger.error(
                    "Explicit supertypes are not supported for module declarations.",
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
