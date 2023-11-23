package xyz.kotlinw.di.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.getClassDeclarationByName
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
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Variance.COVARIANT
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.CodeBlock.Companion
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.VARARG
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import com.squareup.kotlinpoet.typeNameOf
import kotlin.reflect.KClass
import kotlinw.ksp.util.anyReference
import kotlinw.ksp.util.companionClassName
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.ComponentScan
import xyz.kotlinw.di.api.Container
import xyz.kotlinw.di.api.ContainerImplementor
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.di.api.ModuleImplementor
import xyz.kotlinw.di.api.Scope
import xyz.kotlinw.di.api.internal.ContainerImplementorInternal

class DiSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val kspLogger: KSPLogger,
    private val kspOptions: Map<String, String>
) : SymbolProcessor {

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val moduleDeclarations =
            resolver
                .getSymbolsWithAnnotation(Module::class.qualifiedName!!)
                .filterIsInstance<KSClassDeclaration>()
                .toSet()

        val componentDeclarationsFromComponentScan =
            buildMap {
                moduleDeclarations
                    .forEach { moduleDeclaration ->
                        if (moduleDeclaration.isAnnotationPresent(ComponentScan::class)) {
                            put(
                                moduleDeclaration,
                                resolver
                                    .getSymbolsWithAnnotation(Component::class.qualifiedName!!)
                                    .filterIsInstance<KSClassDeclaration>()
                                    .filter {
                                        it.packageName.asString().startsWith(moduleDeclaration.packageName.asString())
                                    }
                                    .toList()
                            )
                        }
                    }
            }

        val containerDeclarations =
            resolver
                .getSymbolsWithAnnotation(Container::class.qualifiedName!!)
                .filterIsInstance<KSClassDeclaration>()
                .toSet()

        val validModuleDeclarations = moduleDeclarations.filter { it.validate() }.toSet()
        val validContainerDeclarations = containerDeclarations.filter { it.validate() }.toSet()
        val validComponentDeclarationsFromComponentScan =
            componentDeclarationsFromComponentScan.mapValues { it.value.filter { it.validate() } }

        validModuleDeclarations.forEach {
            processModuleClass(it, validComponentDeclarationsFromComponentScan[it] ?: emptyList(), resolver)
        }

        validContainerDeclarations.forEach {
            processContainerClass(it, resolver)
        }

        return ((moduleDeclarations + componentDeclarationsFromComponentScan.values.flatten() + containerDeclarations) -
                (validModuleDeclarations + validComponentDeclarationsFromComponentScan.values.flatten() + validContainerDeclarations))
            .toList()
    }

    private fun processContainerClass(containerDeclaration: KSClassDeclaration, resolver: Resolver) {
        val explicitModules =
            containerDeclaration.annotations.filter { it.annotationType.toTypeName() == typeNameOf<Container>() }
                .first().arguments.filter { it.name!!.getShortName() == "modules" }
                .flatMap { it.value as List<KSType> }

        val availableModules = mutableSetOf<KSType>()
        explicitModules.forEach {
            collectAvailableModules(it, availableModules)
        }

        val containerClassName = containerDeclaration.toClassName()
        FileSpec
            .builder(
                containerClassName.packageName,
                containerClassName.simpleName + "Implementor"
            )
            .addFunction(
                FunSpec
                    .builder("createInstance")
                    .receiver(containerClassName.companionClassName())
                    .returns(typeNameOf<ContainerImplementor>())
                    .addCode(
                        """
                            return %T(mapOf(${availableModules.joinToString { "%T::class to TODO()" }}))
                        """.trimIndent(),
                        typeNameOf<ContainerImplementorInternal>(),
                        *availableModules.map { it.toTypeName() }.toTypedArray()
                    )
                    .build()
            )
            .build()
            .writeTo(codeGenerator, true)
    }

    private fun collectAvailableModules(moduleType: KSType, availableModules: MutableSet<KSType>) {
        if (!availableModules.contains(moduleType)) {
            availableModules.add(moduleType)

            moduleType.declaration.annotations.filter { it.annotationType.toTypeName() == typeNameOf<Module>() }
                .first().arguments.filter { it.name!!.getShortName() == "includeModules" }
                .flatMap { it.value as List<KSType> }
                .forEach {
                    collectAvailableModules(it, availableModules)
                }
        }
    }

    @OptIn(KspExperimental::class)
    private fun processModuleClass(
        moduleDeclaration: KSClassDeclaration,
        componentDeclarationsFromComponentScan: List<KSClassDeclaration>,
        resolver: Resolver
    ) {
        if (moduleDeclaration.classKind == CLASS) {
            if (moduleDeclaration.superTypes
                    .filter { it.resolve() != resolver.builtIns.anyType }
                    .toList()
                    .isEmpty()
            ) {
                moduleDeclaration.getDeclaredFunctions()
                    .filter { it.isAnnotationPresent(Component::class) }
                    .forEach {
                        kspLogger.warn("xxx: " + it.simpleName.asString())
                    }

                val moduleClassName = moduleDeclaration.toClassName()

                FileSpec
                    .builder(
                        moduleClassName.packageName,
                        moduleClassName.simpleName + "Implementor"
                    )
                    .build()
                    .writeTo(codeGenerator, true)
            } else {
                kspLogger.error(
                    "Explicit supertypes are not supported for module declarations.",
                    moduleDeclaration
                )
            }
        } else {
            kspLogger.error(
                "Module class should be a normal 'class', '${moduleDeclaration.classKind.toDisplayName()}' is not supported as module declaration.",
                moduleDeclaration
            )
        }
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
