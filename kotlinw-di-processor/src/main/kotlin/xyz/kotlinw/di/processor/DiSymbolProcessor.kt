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
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.LATEINIT
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.KModifier.SUSPEND
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import kotlinw.graph.algorithm.reverseTopologicalSort
import kotlinw.graph.model.DirectedGraph
import kotlinw.graph.model.Vertex
import kotlinw.graph.model.build
import kotlinw.ksp.util.companionClassName
import kotlinw.ksp.util.getAnnotationsOfType
import kotlinw.ksp.util.getArgumentOrNull
import kotlinw.ksp.util.getArgumentValueOrNull
import kotlinw.ksp.util.hasCompanionObject
import kotlinw.ksp.util.isSuspend
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.ComponentScan
import xyz.kotlinw.di.api.Container
import xyz.kotlinw.di.api.ContainerScope
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.di.api.OnConstruction
import xyz.kotlinw.di.api.OnTerminate
import xyz.kotlinw.di.api.Scope
import xyz.kotlinw.di.api.internal.ComponentDependencyKind
import xyz.kotlinw.di.api.internal.ComponentId
import xyz.kotlinw.di.api.internal.ScopeInternal

@OptIn(KspExperimental::class)
class DiSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val kspLogger: KSPLogger,
    private val kspOptions: Map<String, String>
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val componentDeclarations = resolver.getSymbolsWithAnnotation(Component::class.qualifiedName!!).toList()
        val moduleDeclarations = resolver.getSymbolsWithAnnotation(Module::class.qualifiedName!!).toList()
        val containerDeclarations = resolver.getSymbolsWithAnnotation(Container::class.qualifiedName!!).toList()

        val invalidSymbols =
            componentDeclarations.filter { !it.validate() } +
                    moduleDeclarations.filter { !it.validate() } +
                    containerDeclarations.filter { !it.validate() }

        return if (invalidSymbols.isEmpty()) {
            containerDeclarations.forEach {
                processContainerDeclaration(it, resolver)
            }
            emptyList()
        } else {
            componentDeclarations + moduleDeclarations + containerDeclarations
        }
    }

    private fun processContainerDeclaration(containerDeclaration: KSAnnotated, resolver: Resolver) {
        // TODO check not referenced modules
        // TODO check not referenced components
        // TODO check overlapping scanning modules
        // TODO validate component dependencies: everything is KSClassDeclaration
        // TODO validate scanned components: has primary constructor
        // TODO validate scanned components: has no type parameters
        // TODO validate inline components: return type is resolvable
        // TODO validate: container declaration should be an interface with no supertypes
        // TODO validate: container declaration should have only member functions annotated with @Scope or @ComponentLookup
        // TODO validate: @PrecompiledScope.modules nem lehet üres
        // TODO validate: nincs körkörös hivatkozás a scope-oknál

        // TODO        resolver.getSymbolsWithAnnotation(Component::class.qualifiedName!!)
//            .filter {
//                if (it is KSClassDeclaration) {
//                    true
//                } else {
//                    kspLogger.error(
//                        "Invalid component declaration, annotation @${Component::class.simpleName} is allowed only on class declarations.",
//                        it
//                    )
//                    false
//                }
//            } as Sequence<KSClassDeclaration>

        // TODO validate container model (e.g. all dependencies are resolvable with correct arity)

        // TODO module class should be class with no-arg constructor

        val containerModel =
            if (containerDeclaration is KSClassDeclaration && containerDeclaration.classKind == INTERFACE) {
                if (containerDeclaration.hasCompanionObject) {
                    val scopeDeclarations =
                        containerDeclaration
                            .getAllFunctions()
                            .filter { it.isAnnotationPresent(Scope::class) }
                            .mapNotNull { scopeDeclarationFunction ->
                                val scopeDeclarationTypeReference = scopeDeclarationFunction.returnType
                                if (scopeDeclarationTypeReference != null) {
                                    val scopeInterfaceDeclaration = validateScopeInterfaceDeclaration(
                                        scopeDeclarationTypeReference.resolve().declaration,
                                        resolver
                                    )
                                    if (scopeInterfaceDeclaration != null) {
                                        val scopeAnnotation =
                                            scopeDeclarationFunction.getAnnotationsOfType<Scope>().first()
                                        val declaredModulesValue = scopeAnnotation.getArgumentOrNull("modules")
                                        val declaredModules =
                                            declaredModulesValue?.value as? List<KSType> ?: emptyList()
                                        val parentScopeName =
                                            (scopeAnnotation.getArgumentValueOrNull("parent") as? String)?.let { it.ifEmpty { null } }

                                        if (declaredModules.all {
                                                it.declaration is KSClassDeclaration
                                                        && it.declaration.isAnnotationPresent(Module::class)
                                            }
                                        ) {
                                            ScopeModel(
                                                parentScopeName,
                                                scopeDeclarationFunction.simpleName.asString(),
                                                scopeDeclarationFunction,
                                                scopeInterfaceDeclaration,
                                                declaredModules.map {
                                                    ModuleReference(
                                                        it.declaration as KSClassDeclaration,
                                                        (it.declaration as KSClassDeclaration).getModuleId()
                                                    )
                                                },
                                                collectAllModules(declaredModules)
                                                    .map {
                                                        ModuleReference(
                                                            it,
                                                            it.getModuleId()
                                                        )
                                                    }
                                                    .toSet()
                                            )
                                        } else {
                                            val invalidReferences =
                                                declaredModules.filter { it.declaration is KSClassDeclaration }
                                            kspLogger.error(
                                                "Invalid reference(s) to non-module declaration(s): ${
                                                    invalidReferences.joinToString { it.toTypeName().toString() }
                                                }",
                                                scopeDeclarationTypeReference
                                            )
                                            null
                                        }
                                    } else {
                                        null
                                    }
                                } else {
                                    kspLogger.error("Failed to resolve scope type.", scopeDeclarationFunction)
                                    null
                                }
                            }
                            .toList()

                    val moduleDeclarations =
                        scopeDeclarations
                            .flatMap { it.allModules }
                            .toSet()
                            .mapNotNull { processModuleReference(it, resolver) }

                    val overlappingModules = mutableListOf<Pair<ModuleModel, ModuleModel>>()
                    moduleDeclarations.forEach { currentModule ->
                        moduleDeclarations.forEach { referenceModule ->
                            if (currentModule != referenceModule
                                && currentModule.componentScanPackageName != null
                                && referenceModule.componentScanPackageName != null
                                && !overlappingModules.contains(referenceModule to currentModule)
                                && !overlappingModules.contains(currentModule to referenceModule)
                                && (currentModule.componentScanPackageName.startsWith(referenceModule.componentScanPackageName))
                            ) {
                                overlappingModules.add(currentModule to referenceModule)
                            }
                        }
                    }

                    if (overlappingModules.isEmpty()) {
                        ContainerModel(
                            containerDeclaration.qualifiedName!!.asString(),
                            scopeDeclarations,
                            moduleDeclarations,
                            containerDeclaration
                        )
                    } else {
                        overlappingModules.forEach {
                            kspLogger.error("Overlapping modules with @${ComponentScan::class.simpleName}: ${it.first.id} and ${it.second.id}")
                        }
                        null
                    }
                } else {
                    kspLogger.error(
                        "Container declaration interface should have a companion object.",
                        containerDeclaration
                    )
                    null
                }
            } else {
                kspLogger.error("Container declaration should be an `interface`.", containerDeclaration)
                null
            }

        if (containerModel != null) {
            val resolvedContainerModel = resolveContainerModel(containerModel)

            // TODO DAG ellenőrzése

            val codeGenerationModel = createCodeGenerationModel(resolvedContainerModel)

            FileSpec
                .builder(codeGenerationModel.implementationName)
                .addTypes(
                    codeGenerationModel.scopes.values.map { resolvedScopeModel ->
                        generateScopeClass(resolvedScopeModel)
                    }
                )
                .addType(
                    TypeSpec
                        .classBuilder(codeGenerationModel.implementationName)
                        .addSuperinterface(codeGenerationModel.interfaceName)
                        .addModifiers(PRIVATE)
                        .addFunctions(
                            codeGenerationModel.scopes.values.map { scopeCodeGenerationModel ->
                                generateScopeBuilderFunction(scopeCodeGenerationModel)
                            }
                        )
                        .build()
                )
                .addFunction(
                    FunSpec
                        .builder("create")
                        .receiver(codeGenerationModel.interfaceName.companionClassName())
                        .returns(codeGenerationModel.interfaceName)
                        .addStatement("return %T()", codeGenerationModel.implementationName)
                        .build()
                )
                .build()
                .writeTo(codeGenerator, true)
        }
    }

    private fun createCodeGenerationModel(resolvedContainerModel: ResolvedContainerModel): ContainerCodeGenerationModel {
        val scopes = mutableMapOf<ScopeId, ScopeCodeGenerationModel>()
        resolvedContainerModel.scopes.forEach { (scopeId, resolvedScopeModel) ->
            val componentGraph = buildComponentGraph(resolvedScopeModel)
            scopes[scopeId] = ScopeCodeGenerationModel(
                resolvedScopeModel,
                resolvedScopeModel.parentScopeModel?.let { scopes.getValue(it.scopeModel.name) },
                resolvedScopeModel.scopeModel.scopeInterfaceDeclaration.toClassName()
                    .peerClass(resolvedScopeModel.scopeModel.scopeInterfaceDeclaration.simpleName.asString() + "Impl"),
                resolvedScopeModel.modules
                    .filterValues { it.moduleModel.components.any { it is InlineComponentModel } }
                    .keys
                    .mapIndexed { index, moduleId -> moduleId to "m$index" } // TODO beszédes neveket, hogy a generált kód olvashatóbb legyen
                    .toMap(),
                componentGraph,
                componentGraph
                    .reverseTopologicalSort()
                    .filter { it.data in resolvedScopeModel.components }
                    .mapIndexed { index, componentVertex -> componentVertex.data to "c$index" }// TODO beszédes neveket, hogy a generált kód olvashatóbb legyen
                    .toMap()
            )
        }

        val containerInterfaceName = resolvedContainerModel.containerModel.declaration.toClassName()
        return ContainerCodeGenerationModel(
            containerInterfaceName,
            containerInterfaceName.peerClass(containerInterfaceName.simpleName + "Impl"),
            scopes
        )
    }

    private fun generateScopeBuilderFunction(scopeCodeGenerationModel: ScopeCodeGenerationModel) =
        FunSpec
            .builder(scopeCodeGenerationModel.resolvedScopeModel.scopeModel.scopeDeclarationFunction.simpleName.asString())
            .addModifiers(OVERRIDE)
            .returns(scopeCodeGenerationModel.resolvedScopeModel.scopeModel.scopeInterfaceDeclaration.toClassName())
            .apply {
                scopeCodeGenerationModel.parentScopeCodeGenerationModel?.also {
                    addParameter(
                        "parentScope", // TODO it.resolvedScopeModel.scopeModel.scopeDeclarationFunction.parameters.first().name!!.asString(),
                        it.resolvedScopeModel.scopeModel.scopeInterfaceDeclaration.toClassName()
                    )
                }

                if (scopeCodeGenerationModel.parentScopeCodeGenerationModel == null) {
                    addStatement(
                        "return %T()",
                        scopeCodeGenerationModel.implementationClassName
                    )
                } else {
                    addStatement(
                        "return %T(%N as %T)",
                        scopeCodeGenerationModel.implementationClassName,
                        "parentScope", // TODO tuti ez a neve?
                        scopeCodeGenerationModel.parentScopeCodeGenerationModel.implementationClassName
                    )
                }
            }
            .build()

    private fun generateScopeClass(scopeCodeGenerationModel: ScopeCodeGenerationModel): TypeSpec {
        val resolvedScopeModel = scopeCodeGenerationModel.resolvedScopeModel

        fun generateComponentConstructorArguments(
            dependencyCandidates: Map<String, ResolvedComponentDependencyModel>,
            availableDependencies: Map<String, List<ComponentId>>
        ) = dependencyCandidates.values.joinToString {
            val dependencies = availableDependencies.getValue(it.dependencyName)
            if (it.dependencyKind.isMultiple) {
                """listOf(${dependencies.joinToString { scopeCodeGenerationModel.generateComponentAccessor(it) }})"""
            } else {
                if (dependencies.isNotEmpty()) {
                    scopeCodeGenerationModel.generateComponentAccessor(dependencies.first())
                } else {
                    "null"
                }
            }
        }

        fun CodeBlock.Builder.buildScopeInitializer() {
            scopeCodeGenerationModel.componentVariableMap.forEach {
                val componentId = it.key
                val componentVariableName = it.value

                val componentModel = resolvedScopeModel.components.getValue(componentId)
                val dependencies = componentModel.resolveDependenciesInScope()

                addStatement(
                    """
                        $componentVariableName = ${
                        when (componentModel.componentModel) {
                            is ComponentClassModel -> "%T"
                            is InlineComponentModel -> "${
                                scopeCodeGenerationModel.moduleVariableMap.getValue(
                                    componentId.moduleId
                                )
                            }.${componentModel.componentModel.factoryMethodName}"
                        }
                    }(${
                        generateComponentConstructorArguments(
                            componentModel.dependencyCandidates,
                            dependencies
                        )
                    })${
                        if (componentModel.componentModel.lifecycleModel.constructionFunction != null)
                            ".apply { ${componentModel.componentModel.lifecycleModel.constructionFunction!!.simpleName.asString()}() }"
                        else
                            ""
                    }
                    """.trimIndent(),
                    *(
                            if (componentModel.componentModel is ComponentClassModel)
                                listOf(componentModel.componentModel.componentClassDeclaration.toClassName())
                            else
                                emptyList()
                            ).toTypedArray()
                )
            }
        }

        return TypeSpec.classBuilder(scopeCodeGenerationModel.implementationClassName)
            .addSuperinterface(resolvedScopeModel.scopeModel.scopeInterfaceDeclaration.toClassName())
            .addModifiers(PRIVATE)
            .apply {
                if (scopeCodeGenerationModel.parentScopeCodeGenerationModel != null) {
                    primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addParameter(
                                "parentScope",
                                scopeCodeGenerationModel.parentScopeCodeGenerationModel.implementationClassName
                            )
                            .build()
                    )
                }
            }
            .addProperties(
                scopeCodeGenerationModel.moduleVariableMap.map { (moduleId, propertyName) ->
                    val moduleModel = resolvedScopeModel.modules.getValue(moduleId)
                    PropertySpec
                        .builder(propertyName, moduleModel.moduleModel.declaringClass.toClassName())
                        .initializer("%T()", moduleModel.moduleModel.declaringClass.toClassName())
                        .build()
                }
            )
            .addProperties(
                scopeCodeGenerationModel.componentVariableMap
                    .map { (componentId, propertyName) ->
                        PropertySpec.builder(
                            propertyName,
                            resolvedScopeModel.components.getValue(componentId).componentModel.componentType.toTypeName(),
                            LATEINIT
                        )
                            .mutable(true)
                            .build()
                    }
            )
            .addFunction(
                FunSpec.builder(ScopeInternal::start.name)
                    .addModifiers(OVERRIDE, SUSPEND)
                    .addCode(
                        CodeBlock.builder().apply { buildScopeInitializer() }.build()
                    )
                    .build()
            )
            .addFunction(
                FunSpec.builder(ScopeInternal::close.name)
                    .addModifiers(OVERRIDE, SUSPEND)
                    .build()
            )
            .build()
    }

    private fun processModuleReference(moduleReference: ModuleReference, resolver: Resolver): ModuleModel? {
        val moduleDeclaration = moduleReference.moduleDeclaration
        val moduleId = moduleDeclaration.getModuleId()

        val inlineComponents =
            moduleDeclaration
                .findInlineComponents()
                .map { processInlineComponent(it, moduleId) }
                .toList()

        val componentScanPackageName =
            if (moduleDeclaration.isAnnotationPresent(ComponentScan::class)) moduleDeclaration.packageName.asString() else null

        val componentClasses =
            if (componentScanPackageName != null)
                findComponentsByComponentScan(resolver, componentScanPackageName)
                    .map { processComponentClass(it, moduleId) }
                    .toList()
            else
                emptyList()

        val hasInvalidComponents = (inlineComponents + componentClasses).contains(null)

        return if (!hasInvalidComponents) {
            @Suppress("UNCHECKED_CAST")
            ModuleModel(
                moduleId,
                moduleDeclaration,
                inlineComponents as List<ComponentModel> + componentClasses as List<ComponentModel>,
                componentScanPackageName
            )
        } else {
            kspLogger.error("Some components declared by this module are invalid.", moduleDeclaration)
            null
        }
    }

    private fun processComponentClass(
        componentClassDeclaration: KSClassDeclaration,
        moduleId: String
    ): ComponentModel? =
        if (componentClassDeclaration.qualifiedName != null) {
            if (componentClassDeclaration.typeParameters.isEmpty()) {
                if (componentClassDeclaration.primaryConstructor != null) {
                    val onConstructionFunctions = componentClassDeclaration.getAllFunctions()
                        .filter { it.isAnnotationPresent(OnConstruction::class) }.toList()
                    val onTerminationFunctions = componentClassDeclaration.getAllFunctions()
                        .filter { it.isAnnotationPresent(OnTerminate::class) }.toList()
                    if (onConstructionFunctions.size <= 1 && onTerminationFunctions.size <= 1) {
                        ComponentClassModel(
                            ComponentId(moduleId, componentClassDeclaration.qualifiedName!!.asString()),
// TODO
//                            componentClassDeclaration.getAnnotationsOfType<Component>().first()
//                                .getArgumentValueOrNull("type") as? KSType ?:
                                componentClassDeclaration.asType(emptyList()),
                            componentClassDeclaration.primaryConstructor!!.parameters.associate {
                                it.name!!.asString() to it.type.resolve().toComponentLookup()
                            },
                            componentClassDeclaration,
                            ComponentLifecycleModel(
                                onConstructionFunctions.firstOrNull(),
                                onTerminationFunctions.firstOrNull()
                            )
                        )
                    } else {
                        if (onConstructionFunctions.size > 1) {
                            kspLogger.error(
                                "Component shouldn't have multiple functions annotated with ${annotationDisplayName<OnConstruction>()}.",
                                componentClassDeclaration
                            )
                        }
                        if (onTerminationFunctions.size > 1) {
                            kspLogger.error(
                                "Component shouldn't have multiple functions annotated with ${annotationDisplayName<OnTerminate>()}.",
                                componentClassDeclaration
                            )
                        }
                        null
                    }
                } else {
                    kspLogger.error("Component should have a primary constructor.", componentClassDeclaration)
                    null
                }
            } else {
                kspLogger.error("Component should have no type parameters.", componentClassDeclaration)
                null
            }
        } else {
            kspLogger.error("Component should be a top-level class.", componentClassDeclaration)
            null
        }

    private fun processInlineComponent(
        inlineComponentDeclaration: KSFunctionDeclaration,
        moduleId: String
    ): ComponentModel? =
        if (inlineComponentDeclaration.returnType != null) {
            InlineComponentModel(
                ComponentId(moduleId, inlineComponentDeclaration.simpleName.asString()),
                inlineComponentDeclaration.returnType!!.resolve(),
                inlineComponentDeclaration.parameters.associate {
                    it.name!!.asString() to it.type.resolve().toComponentLookup()
                },
                inlineComponentDeclaration,
                ComponentLifecycleModel(null, null) // TODO allow
            )
        } else {
            kspLogger.error("Failed to resolve component type.", inlineComponentDeclaration)
            null
        }

    private fun validateScopeInterfaceDeclaration(
        scopeInterfaceDeclaration: KSDeclaration,
        resolver: Resolver
    ): KSClassDeclaration? =
        if (scopeInterfaceDeclaration is KSClassDeclaration) {
            if (scopeInterfaceDeclaration.classKind == INTERFACE) {
                if (scopeInterfaceDeclaration.typeParameters.isEmpty()) {
                    if (resolver
                            .getClassDeclarationByName<ContainerScope>()!!
                            .asType(emptyList())
                            .isAssignableFrom(scopeInterfaceDeclaration.asType(emptyList()))
                    ) {
                        scopeInterfaceDeclaration
                    } else {
                        kspLogger.error(
                            "Scope interface must extend `${ContainerScope::class.simpleName}`.",
                            scopeInterfaceDeclaration
                        )
                        null
                    }
                } else {
                    kspLogger.error(
                        "Scope declaration should be an `interface` with no type parameters.",
                        scopeInterfaceDeclaration
                    )
                    null
                }
            } else {
                kspLogger.error(
                    "Scope declaration should be an `interface`, `${scopeInterfaceDeclaration.classKind.toDisplayName()}` is not supported.",
                    scopeInterfaceDeclaration
                )
                null
            }
        } else {
            kspLogger.error("Scope declaration should be an `interface`.", scopeInterfaceDeclaration)
            null
        }

    private fun buildComponentGraph(resolvedScopeModel: ResolvedScopeModel): DirectedGraph<ComponentId, Vertex<ComponentId>> =
        DirectedGraph.build {
            val components = resolvedScopeModel.collectComponents()
            val vertexes = components.keys.associateWith { vertex(it) }

            components.forEach {
                val componentId = it.key
                it.value.dependencyCandidates
                    .forEach {
                        it.value.candidates
                            .forEach {
                                edge(vertexes.getValue(componentId), vertexes.getValue(it.component.id))
                            }
                    }
            }
        }

    // TODO ez használhatná belül a ComponentLookup-ot
    private fun ResolvedComponentModel.resolveDependenciesInScope(): Map<String, List<ComponentId>> {
        return dependencyCandidates.values.associate {
            it.dependencyName to it.candidates.map { it.component.id }
        }
    }

    private fun collectAllModules(explicitModules: List<KSType>): Set<KSClassDeclaration> {
        val allModules = mutableSetOf<KSClassDeclaration>()
        explicitModules.forEach {
            collectTransitiveModules(it.declaration as KSClassDeclaration, allModules)
        }
        return allModules
    }

    private fun collectTransitiveModules(
        moduleType: KSClassDeclaration,
        allModules: MutableSet<KSClassDeclaration>
    ) {
        if (!allModules.contains(moduleType)) {
            allModules.add(moduleType)

            (moduleType.getAnnotationsOfType<Module>().first()
                .getArgumentValueOrNull("includeModules") as? List<KSType>)
                ?.forEach {
                    collectTransitiveModules(it.declaration as KSClassDeclaration, allModules)
                }
        }
    }

    private fun KSClassDeclaration.findInlineComponents() =
        getDeclaredFunctions().filter { it.isAnnotationPresent(Component::class) }

    private fun findComponentsByComponentScan(
        resolver: Resolver,
        modulePackageName: String
    ): Sequence<KSClassDeclaration> =
        resolver.getDeclarationsFromPackage(modulePackageName)
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.isAnnotationPresent(Component::class) }

    private fun KSType.toComponentLookup(): ComponentLookup {
        val parameterDeclaration = declaration as KSClassDeclaration
        return if (parameterDeclaration.qualifiedName!!.asString() == List::class.qualifiedName) {
            ComponentLookup(
                arguments[0].type!!.resolve(),
                ComponentDependencyKind.MULTIPLE_OPTIONAL
            ) // TODO handle MULITPLE_REQUIRED
        } else if (isMarkedNullable) {
            ComponentLookup(this, ComponentDependencyKind.SINGLE_OPTIONAL)
        } else {
            ComponentLookup(this, ComponentDependencyKind.SINGLE_REQUIRED)
        }
    }

    private fun resolveContainerModel(containerModel: ContainerModel): ResolvedContainerModel {
        val resolvedScopes = mutableMapOf<String, ResolvedScopeModel>()
        containerModel.scopes
            .sortedWith { o1, o2 ->
                if (o1.parentScopeName == o2.name) {
                    1
                } else if (o1.name == o2.parentScopeName) {
                    -1
                } else {
                    0
                }
            }
            .forEach { scope ->
                val parentScopeModuleIds =
                    scope.parentScopeName?.let { resolvedScopes.getValue(it).modules.keys } ?: emptyList()
                resolvedScopes[scope.name] =
                    scope.resolve(
                        containerModel.modules
                            .filter { scope.allModules.map { it.moduleId }.contains(it.id) }
                            .filter { !parentScopeModuleIds.contains(it.id) }
                            .map { ResolvedModuleModel(it, scope.name) },
                        scope.parentScopeName?.let { resolvedScopes.getValue(it) }
                    )
            }

        return ResolvedContainerModel(containerModel, resolvedScopes.values.associateBy { it.scopeModel.name })
    }

    private fun ScopeModel.resolve(
        scopeModules: List<ResolvedModuleModel>,
        parentScopeModel: ResolvedScopeModel?
    ): ResolvedScopeModel =
        ResolvedScopeModel(
            this,
            parentScopeModel,
            scopeModules.associateBy { it.moduleModel.id },
            scopeModules
                .flatMap {
                    it.moduleModel.components.map {
                        ResolvedComponentModel(
                            it,
                            name,
                            it.dependencyDefinitions.map {
                                ResolvedComponentDependencyModel(
                                    it.key,
                                    it.value.type,
                                    it.value.dependencyKind,
                                    resolve(
                                        scopeModules + (parentScopeModel?.modules?.values ?: emptyList()),
                                        it.value
                                    ) // TODO több szintű parent-et is lekövetni rekurzívan
                                )
                            }.associateBy { it.dependencyName }
                        )
                    }
                }
                .associateBy { it.componentModel.id }
        )

    private fun resolve(
        scopeModules: List<ResolvedModuleModel>,
        componentLookup: ComponentLookup
    ): List<ComponentDependencyCandidate> = // TODO sokkal hatékonyabbra
        scopeModules
            .flatMap { moduleModel ->
                moduleModel.moduleModel.components.map { ComponentDependencyCandidate(moduleModel.moduleModel, it) }
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

    private fun KSClassDeclaration.getModuleId() = qualifiedName!!.asString()
}

private fun ResolvedScopeModel.collectComponents(): Map<ComponentId, ResolvedComponentModel> =
    components + (parentScopeModel?.collectComponents() ?: emptyMap())

private inline fun <reified T : Annotation> annotationDisplayName(): String = "@" + T::class.simpleName
