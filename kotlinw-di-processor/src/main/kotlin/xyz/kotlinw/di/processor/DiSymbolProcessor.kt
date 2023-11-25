package xyz.kotlinw.di.processor

import com.google.devtools.ksp.KspExperimental
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
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import kotlinw.graph.algorithm.isAcyclic
import kotlinw.graph.algorithm.reverseTopologicalSort
import kotlinw.graph.algorithm.topologicalSort
import kotlinw.graph.model.DirectedGraph
import kotlinw.graph.model.Vertex
import kotlinw.graph.model.build
import kotlinw.ksp.util.companionClassName
import kotlinw.ksp.util.getAnnotationsOfType
import kotlinw.ksp.util.getArgumentOrNull
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.ComponentQuery
import xyz.kotlinw.di.api.ComponentScan
import xyz.kotlinw.di.api.Container
import xyz.kotlinw.di.api.DynamicContainer
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.di.api.PrecompiledScope
import xyz.kotlinw.di.api.internal.ComponentDependencyCandidateMetadata
import xyz.kotlinw.di.api.internal.ComponentDependencyKind
import xyz.kotlinw.di.api.internal.ComponentId
import xyz.kotlinw.di.api.internal.ComponentMetadata
import xyz.kotlinw.di.api.internal.ContainerImplementorInternal
import xyz.kotlinw.di.api.internal.ContainerMetadata
import xyz.kotlinw.di.api.internal.ModuleMetadata

@OptIn(KspExperimental::class)
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
            moduleDeclarations.associateWith { it.findInlineComponents().toList() }

        val containerDeclarations =
            resolver
                .getSymbolsWithAnnotation(Container::class.qualifiedName!!)
                .filterIsInstance<KSClassDeclaration>()
                .toSet()

        val validModuleDeclarations = moduleDeclarations.filter { it.validate() }.toSet()
        val validContainerDeclarations = containerDeclarations.filter { it.validate() }.toSet()
        val validComponentDeclarationsFromComponentScan =
            componentDeclarationsFromComponentScan.mapValues { it.value.filter { it.validate() } }

        if (validModuleDeclarations.all { isModuleClassValid(it, resolver) }) {
            validContainerDeclarations.forEach {
                processContainerClass(it, resolver)
            }
        }

        return ((moduleDeclarations + componentDeclarationsFromComponentScan.values.flatten() + containerDeclarations) -
                (validModuleDeclarations + validComponentDeclarationsFromComponentScan.values.flatten() + validContainerDeclarations))
            .toList()
    }

    private fun processContainerClass(containerDeclaration: KSClassDeclaration, resolver: Resolver) {
        val explicitModules = containerDeclaration.getAnnotationsOfType<Container>().first()
            .getArgumentOrNull("modules")!!.value as List<KSType>

        val availableModules = collectTransitiveModules(explicitModules)

        val modulesWithComponentScan = availableModules.filter { it.isAnnotationPresent(ComponentScan::class) }
        val overlappingModules = mutableSetOf<Pair<KSClassDeclaration, KSClassDeclaration>>()
        modulesWithComponentScan.forEach { validatedModuleType ->
            (modulesWithComponentScan - validatedModuleType).forEach { currentModuleType ->
                if (validatedModuleType.packageName.asString()
                        .startsWith(currentModuleType.packageName.asString())
                    && !overlappingModules.contains(validatedModuleType to currentModuleType)
                    && !overlappingModules.contains(currentModuleType to validatedModuleType)
                ) {
                    overlappingModules.add(validatedModuleType to currentModuleType)
                }
            }
        }

        if (overlappingModules.isNotEmpty()) {
            overlappingModules.forEach {
                kspLogger.error("Overlapping modules with ${ComponentScan::class.simpleName}: ${it.first.qualifiedName?.asString()} and ${it.second.qualifiedName?.asString()}")
            }
            return
        }

        // TODO validate component dependencies: everything is KSClassDeclaration
        // TODO validate scanned components: has primary constructor
        // TODO validate scanned components: has no type parameters
        // TODO validate inline components: return type is resolvable
        // TODO validate: container declaration should be an interface with no supertypes
        // TODO validate: container declaration should have only member functions annotated with @Scope or @ComponentLookup
        // TODO validate: @PrecompiledScope.modules nem lehet üres

        val containerModel = ContainerModel(
            containerDeclaration.qualifiedName!!.asString(),
            availableModules.map { module ->
                val moduleId = module.qualifiedName!!.asString()
                ModuleModel(
                    moduleId,
                    module,
                    module.findInlineComponents().map {
                        ComponentModel(
                            ComponentId(moduleId, it.simpleName.asString()),
                            true,
                            it.returnType!!.resolve(),
                            (it.getAnnotationsOfType<Component>().first()
                                .getArgumentOrNull("type")?.value as? KSType)
                                ?: it.returnType!!.resolve(), // TODO type nem lehet akármi, kompatibilisnek kell lennie
                            it.parameters.map { it.type.resolve().toComponentLookup() }
                        )
                    }.toList() +
                            module.findComponentsByComponentScan(resolver).map {
                                ComponentModel(
                                    ComponentId(moduleId, it.qualifiedName!!.asString()),
                                    false,
                                    it.asType(emptyList()),
                                    (it.getAnnotationsOfType<Component>().first()
                                        .getArgumentOrNull("type")?.value as? KSType)
                                        ?: it.asType(emptyList()), // TODO type nem lehet akármi, kompatibilisnek kell lennie
                                    it.primaryConstructor!!.parameters.map { it.type.resolve().toComponentLookup() }
                                )
                            }.toList()
                )
            }
        )

        val resolvedContainerModel = containerModel.resolve(resolver)
        kspLogger.warn(resolvedContainerModel.toString()) // TODO törölni
        // TODO validate container model (e.g. all dependencies are resolvable with correct arity)

        val containerInterfaceName = containerDeclaration.toClassName()
        val containerImplementorClassName = containerInterfaceName.peerClass(containerInterfaceName.simpleName + "Impl")

        FileSpec
            .builder(
                containerInterfaceName.packageName,
                containerImplementorClassName.simpleName
            )
            .addType(
                TypeSpec
                    .classBuilder(containerImplementorClassName)
                    .superclass(ContainerImplementorInternal::class)
                    .addSuperinterface(containerInterfaceName)
                    .addSuperclassConstructorParameter("containerMetadata")
                    .primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addParameter(
                                "containerMetadata",
                                resolver.getClassDeclarationByName<ContainerMetadata>()!!.asType(emptyList())
                                    .makeNullable().toTypeName()
                            )
                            .build()
                    )
                    .addFunctions(
                        containerDeclaration.getDeclaredFunctions().map { overriddenFunction ->
                            FunSpec
                                .builder(overriddenFunction.simpleName.asString())
                                .addModifiers(OVERRIDE)
                                .addParameters(overriddenFunction.parameters.map {
                                    ParameterSpec(
                                        it.name!!.asString(),
                                        it.type.toTypeName()
                                    )
                                })
                                .returns(overriddenFunction.returnType!!.toTypeName())
                                .apply {
                                    if (overriddenFunction.isAnnotationPresent(PrecompiledScope::class)) {
                                        val precompiledScopeAnnotation =
                                            overriddenFunction.getAnnotationsOfType<PrecompiledScope>().first()

                                        val parentScopeName =
                                            precompiledScopeAnnotation.getArgumentOrNull("parentScope")!!.value as? String

                                        val explicitScopeModules =
                                            precompiledScopeAnnotation.getArgumentOrNull("modules")!!.value as List<KSType>
                                        val scopeModuleTypes = collectTransitiveModules(explicitScopeModules)

                                        val componentGraph =
                                            buildComponentGraph(
                                                resolvedContainerModel,
                                                scopeModuleTypes.toSet()
                                            )

                                        if (componentGraph.isAcyclic()) {
                                            componentGraph.reverseTopologicalSort().forEach {
                                                kspLogger.warn(it.data.toString())
                                            }
                                        } else {
                                            kspLogger.error("Cycle component dependency found.", containerDeclaration)
                                        }
                                    } else if (overriddenFunction.isAnnotationPresent(ComponentQuery::class)) {
                                        addComment(
                                            "" + containerModel.resolve(
                                                overriddenFunction.returnType!!.resolve().toComponentLookup()
                                            )
                                        )
                                        addCode("TODO()")
                                    } else {
                                        throw AssertionError()
                                    }
                                }
                                .build()
                        }.toList()
                    )
                    .build()
            )
            .addFunction(
                FunSpec
                    .builder("create")
                    .receiver(containerInterfaceName.companionClassName())
                    .returns(containerInterfaceName)
                    .apply {
                        if (resolver.getClassDeclarationByName<DynamicContainer>()!!.asType(emptyList())
                                .isAssignableFrom(containerDeclaration.asType(emptyList()))
                        ) {
                            addStatement(
                                """
                                    val modules: Map<String, Any> = mapOf(${
                                    resolvedContainerModel.availableModules.filter { it.components.any { it.isInline } }
                                        .joinToString { "%S to %T()" }
                                })
                                """.trimIndent(),
                                *resolvedContainerModel.availableModules.filter { it.components.any { it.isInline } }
                                    .flatMap { listOf(it.id, it.declaringClass.toClassName()) }.toTypedArray(),
                            )
                            addStatement(
                                """
                                    return %T(%T(listOf(${resolvedContainerModel.availableModules.joinToString { moduleModel -> "%T(%S, %T::class, listOf(${moduleModel.components.joinToString { componentModel -> "%T<%T>(%S, {${if (componentModel.isInline) "(modules.getValue(%S) as %T).%N(${componentModel.dependencyCandidates.joinToString { "%N[%L] as %T" }})" else "%T(${componentModel.dependencyCandidates.joinToString { "%N[%L] as %T" }})"}}, listOf(${componentModel.dependencyCandidates.joinToString { "%T(%T.%N, listOf(${it.candidates.joinToString { "%T(%S, %S)" }}))" }}), {}, {})" }}))" }})))
                                """.trimIndent(),
                                containerImplementorClassName,
                                ContainerMetadata::class,
                                *resolvedContainerModel.availableModules.flatMap { moduleModel ->
                                    listOf(
                                        ModuleMetadata::class,
                                        moduleModel.id,
                                        moduleModel.declaringClass.toClassName()
                                    ) + moduleModel.components.flatMap { componentModel ->
                                        listOf(
                                            ComponentMetadata::class,
                                            componentModel.implementationType.toTypeName(),
                                            componentModel.id.moduleLocalId
                                        ) + (
                                                if (componentModel.isInline) {
                                                    listOf(
                                                        moduleModel.id,
                                                        moduleModel.declaringClass.toClassName(),
                                                        componentModel.id.moduleLocalId // TODO ehelyett egy rendes mezőben legyen eltárolva, hogy mi a function neve, amit meg kell hívni
                                                    ) +
                                                            componentModel.dependencyCandidates.flatMapIndexed { index, dependencyModel ->
                                                                listOf(
                                                                    "it",
                                                                    index,
                                                                    if (dependencyModel.dependencyKind.isMultiple)
                                                                        List::class.asClassName()
                                                                            .parameterizedBy(dependencyModel.dependencyType.toTypeName())
                                                                    else
                                                                        dependencyModel.dependencyType.toTypeName()
                                                                )
                                                            }
                                                } else {
                                                    listOf(componentModel.implementationType.toClassName()) +
                                                            componentModel.dependencyCandidates.flatMapIndexed { index, dependencyModel ->
                                                                listOf(
                                                                    "it",
                                                                    index,
                                                                    if (dependencyModel.dependencyKind.isMultiple)
                                                                        List::class.asClassName()
                                                                            .parameterizedBy(dependencyModel.dependencyType.toTypeName())
                                                                    else
                                                                        dependencyModel.dependencyType.toTypeName()
                                                                )
                                                            } // TODO ez van kicsit feljebb is
                                                }) +
                                                componentModel.dependencyCandidates.flatMap {
                                                    listOf(
                                                        ComponentDependencyCandidateMetadata::class.asClassName(),
                                                        ComponentDependencyKind::class.asClassName(),
                                                        it.dependencyKind.name
                                                    ) +
                                                            it.candidates.flatMap {
                                                                listOf(
                                                                    ComponentId::class.asClassName(),
                                                                    it.component.id.moduleId,
                                                                    it.component.id.moduleLocalId
                                                                )
                                                            }
                                                }
                                    }
                                }.toTypedArray()
                            )
                        } else {
                            addStatement("return %T(null)", containerImplementorClassName)
                        }
                    }
                    .build()
            )
            .build()
            .writeTo(codeGenerator, true)
    }

    private fun buildComponentGraph(
        resolvedContainerModel: ResolvedContainerModel,
        moduleDeclarations: Set<KSClassDeclaration>
    ): DirectedGraph<ComponentId, Vertex<ComponentId>> =
        DirectedGraph.build {
            val modules = resolvedContainerModel.availableModules.filter { it.declaringClass in moduleDeclarations }
            val components = modules.flatMap { it.components }.associateBy { it.id }
            val vertexes = components.keys.associateWith {
                vertex(it)
            }

            components.forEach {
                val componentId = it.key
                it.value.dependencyCandidates
                    .forEach {
                        it.candidates.forEach {
                            edge(vertexes.getValue(componentId), vertexes.getValue(it.component.id))
                        }
                    }
            }
        }

    private fun collectTransitiveModules(explicitModules: List<KSType>): Set<KSClassDeclaration> {
        val availableModules = mutableSetOf<KSClassDeclaration>()
        explicitModules.forEach {
            collectAvailableModules(it.declaration as KSClassDeclaration, availableModules)
        }
        return availableModules
    }

    private fun collectAvailableModules(
        moduleType: KSClassDeclaration,
        availableModules: MutableSet<KSClassDeclaration>
    ) {
        if (!availableModules.contains(moduleType)) {
            availableModules.add(moduleType)

            (moduleType.getAnnotationsOfType<Module>().first()
                .getArgumentOrNull("includeModules")?.value as List<KSType>?)
                ?.forEach {
                    collectAvailableModules(it.declaration as KSClassDeclaration, availableModules)
                }
        }
    }

    private fun isModuleClassValid(
        moduleDeclaration: KSClassDeclaration,
        resolver: Resolver
    ): Boolean =
        if (moduleDeclaration.classKind == CLASS) {
            if (moduleDeclaration.superTypes
                    .filter { it.resolve() != resolver.builtIns.anyType }
                    .toList()
                    .isEmpty()
            ) {
                moduleDeclaration.findInlineComponents().forEach {
                    // TODO validate
                }
                true
            } else {
                kspLogger.error(
                    "Explicit supertypes are not supported for module declarations.",
                    moduleDeclaration
                )
                false
            }
        } else {
            kspLogger.error(
                "Module class should be a normal 'class', '${moduleDeclaration.classKind.toDisplayName()}' is not supported as module declaration.",
                moduleDeclaration
            )
            false
        }

    private fun KSClassDeclaration.findInlineComponents() =
        getDeclaredFunctions().filter { it.isAnnotationPresent(Component::class) }

    private fun KSClassDeclaration.findComponentsByComponentScan(resolver: Resolver) =
        if (isAnnotationPresent(ComponentScan::class)) {
            resolver.getSymbolsWithAnnotation(Component::class.qualifiedName!!)
                .filterIsInstance<KSClassDeclaration>()
                .filter { it.packageName.asString().startsWith(packageName.asString()) }
        } else {
            emptySequence()
        }

    private fun KSType.toComponentLookup(): ComponentLookup {
        val parameterDeclaration = declaration as KSClassDeclaration
        return if (parameterDeclaration.qualifiedName!!.asString() == List::class.qualifiedName) {
            ComponentLookup(
                arguments[0].type!!.resolve(),
                ComponentDependencyKind.MULTIPLE_OPTIONAL
            ) // TODO handle MULITPLE_REQUIRED
        } else {
            ComponentLookup(this, ComponentDependencyKind.SINGLE_REQUIRED) // TODO handle SINGLE_OPTIONAL
        }
    }

    private fun ContainerModel.resolve(resolver: Resolver) =
        ResolvedContainerModel(
            id,
            availableModules.map {
                ResolvedModuleModel(
                    it.id,
                    it.declaringClass,
                    it.components.map {
                        ResolvedComponentModel(
                            it.id,
                            it.isInline,
                            it.implementationType,
                            it.componentType,
                            it.dependencies.map {
                                ResolvedComponentDependencyModel(
                                    it.type,
                                    it.dependencyKind,
                                    resolve(it)
                                )
                            })
                    }
                )
            }
        )

    private fun ContainerModel.resolve(componentLookup: ComponentLookup): List<ComponentDependencyCandidate> =
        availableModules
            .flatMap { moduleModel ->
                moduleModel.components.map { ComponentDependencyCandidate(moduleModel, it) }
            }.filter {
                componentLookup.type.isAssignableFrom(it.component.componentType)
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
}

data class ContainerModel(val id: String, val availableModules: List<ModuleModel>)

data class ModuleModel(val id: String, val declaringClass: KSClassDeclaration, val components: List<ComponentModel>)

data class ComponentModel(
    val id: ComponentId,
    val isInline: Boolean,
    val implementationType: KSType,
    val componentType: KSType,
    val dependencies: List<ComponentLookup>
)

data class ComponentLookup(val type: KSType, val dependencyKind: ComponentDependencyKind)

data class ResolvedContainerModel(val id: String, val availableModules: List<ResolvedModuleModel>)

data class ResolvedModuleModel(
    val id: String,
    val declaringClass: KSClassDeclaration,
    val components: List<ResolvedComponentModel>
)

data class ResolvedComponentModel(
    val id: ComponentId,
    val isInline: Boolean,
    val implementationType: KSType,
    val componentType: KSType,
    val dependencyCandidates: List<ResolvedComponentDependencyModel>
)

data class ResolvedComponentDependencyModel(
    val dependencyType: KSType,
    val dependencyKind: ComponentDependencyKind,
    val candidates: List<ComponentDependencyCandidate>
)

data class ComponentDependencyCandidate(val declaringModule: ModuleModel, val component: ComponentModel) {

    override fun toString() = "ComponentDependencyCandidate(component.id=${component.id})"
}
