package xyz.kotlinw.di.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
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
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
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
import com.squareup.kotlinpoet.typeNameOf
import kotlinw.graph.algorithm.reverseTopologicalSort
import kotlinw.graph.model.DirectedGraph
import kotlinw.graph.model.Vertex
import kotlinw.graph.model.build
import kotlinw.ksp.util.companionClassName
import kotlinw.ksp.util.getAnnotationsOfType
import kotlinw.ksp.util.getArgumentOrNull
import kotlinw.ksp.util.getArgumentValueOrNull
import kotlinw.ksp.util.hasCompanionObject
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.ComponentQuery
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

// TODO körkörös referencia kezelése @Module.includeModules-ben
// TODO warning, ha egy modul többször van felsorolva
// TODO a container implementációt generálja le akkor is, ha egyébként vannak hibák, hogy a container-t és a scope-okat létrehozó kód még helyes maradjon
// TODO attól, hogy csak container-t tartalmazó Gradle modul esetén generál kódot, ellenőrizni még kellene az adott Gradle modulban deklarált elemeket
// TODO ellenőrizni, hogy @Container interfészben minden metódusnak @Scope-pal annotatáltnak kell lennie
// TODO ellenőrzés, hogy a scope interfészben minden metódus annotatált-e @ComponentQuery-vel
// FIXME hiányzó dependency esetén hibás kódot generál, és nem ad hasznos fordítási hibát
// TODO component query-nél ellenőrizni kellene a metódus elején, hogy el a scope-on meg lett-e már hívva a start()
// TODO ha az includeModules-ben @Module-lal nem annotált class szerepel, akkor elszáll

// TODO tök üres modulra elszáll
//@Container
//interface TestBootstrapContainer {
//
//    companion object
//
//    interface TestScope: ContainerScope
//
//    @Scope(modules = [TestModule::class])
//    fun testScope(): TestScope
//
//    @Module
//    class TestModule
//}
//[ksp] java.lang.IllegalArgumentException: Failed requirement.
//at kotlinw.util.stdlib.MutableBloomFilterImpl.<init>(BloomFilter.kt:50)
//at kotlinw.util.stdlib.BloomFilterKt.newMutableBloomFilter(BloomFilter.kt:30)
//at kotlinw.util.stdlib.BloomFilterKt.newMutableBloomFilter$default(BloomFilter.kt:29)
//at kotlinw.graph.algorithm.TopologicalSortData.<init>(GraphTopologicalSort.kt:39)

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

        val validComponentDeclarations = componentDeclarations.filter { it.validate() }
        val validModuleDeclarations = moduleDeclarations.filter { it.validate() }
        val validContainerDeclarations = containerDeclarations.filter { it.validate() }

        validContainerDeclarations.forEach {
            processContainerDeclaration(it, resolver)
        }

        return (
                (componentDeclarations + moduleDeclarations + containerDeclarations).toSet() -
                        (validComponentDeclarations + validModuleDeclarations + validContainerDeclarations).toSet()
                ).toList()
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
                                                    .toSet(),
                                                collectComponentQueries(scopeInterfaceDeclaration, resolver)
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
                        "Container declaration interface should have a `companion object` (see related Kotlin feature request: https://youtrack.jetbrains.com/issue/KT-11968/Research-and-prototype-namespace-based-solution-for-statics-and-static-extensions ).",
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

    private fun collectComponentQueries(
        scopeInterfaceDeclaration: KSClassDeclaration,
        resolver: Resolver
    ): List<ComponentQueryModel> {
        val parentScopeTypes =
            scopeInterfaceDeclaration.getAllSuperTypes()
                .map { it.declaration }
                .filterIsInstance<KSClassDeclaration>()
                .toList()

        val componentQueryProperties = (scopeInterfaceDeclaration.getDeclaredProperties() +
                parentScopeTypes
                    .flatMap { it.getDeclaredProperties() }
                )
            .filter { it.isAnnotationPresent(ComponentQuery::class) }
            .toList()

        val componentQueryFunctions = (scopeInterfaceDeclaration.getDeclaredFunctions() +
                parentScopeTypes
                    .flatMap { it.getDeclaredFunctions() }
                )
            .filter { it.isAnnotationPresent(ComponentQuery::class) }
            .toList()

        return componentQueryFunctions
            .map {
                val type = it.returnType!!.resolve()
                ComponentQueryModel(it, type, type.toComponentLookup())
            }
            .toList() +
                componentQueryProperties
                    .map {
                        val type = it.type.resolve()
                        ComponentQueryModel(it, type, type.toComponentLookup())
                    }
                    .toList()
    }

    private fun createCodeGenerationModel(resolvedContainerModel: ResolvedContainerModel): ContainerCodeGenerationModel {
        val containerInterfaceName = resolvedContainerModel.containerModel.declaration.toClassName()
        val containerImplementationPackageName = containerInterfaceName.packageName

        val scopes = mutableMapOf<ScopeId, ScopeCodeGenerationModel>()
        resolvedContainerModel.scopes.forEach { (scopeId, resolvedScopeModel) ->
            val componentGraph = buildComponentGraph(resolvedScopeModel)
            val scopeInterfaceDeclaration = resolvedScopeModel.scopeModel.scopeInterfaceDeclaration
            val scopeInterfaceClassName = scopeInterfaceDeclaration.toClassName()
            scopes[scopeId] = ScopeCodeGenerationModel(
                resolvedScopeModel,
                resolvedScopeModel.parentScopeModel?.let { scopes.getValue(it.scopeModel.name) },
                ClassName(
                    containerImplementationPackageName,
                    resolvedScopeModel.scopeModel.name.capitalize() + "Impl"
                ),
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

        return ContainerCodeGenerationModel(
            containerInterfaceName,
            ClassName(containerImplementationPackageName, containerInterfaceName.simpleName + "Impl"),
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

        // TODO jobb nevet, mert a component query generálás is ezt használja
        fun generateComponentConstructorArgument(
            dependencies: List<ComponentId>,
            dependencyKind: ComponentDependencyKind
        ): String {
            return if (dependencyKind.isMultiple) {
                """listOf(${dependencies.joinToString { scopeCodeGenerationModel.generateComponentAccessor(it) }})"""
            } else {
                if (dependencies.isNotEmpty()) {
                    scopeCodeGenerationModel.generateComponentAccessor(dependencies.first())
                } else {
                    "null"
                }
            }
        }

        fun generateComponentConstructorArguments(
            dependencyCandidates: Map<String, ResolvedComponentDependencyModel>,
            availableDependencies: Map<String, List<ComponentId>>
        ) = dependencyCandidates.values.joinToString {
            generateComponentConstructorArgument(availableDependencies.getValue(it.dependencyName), it.dependencyKind)
        }

        fun CodeBlock.Builder.generateStartMethod() {
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

        fun TypeSpec.Builder.generateCloseMethod() {
            // TODO kezelni, ha a close() már meg lett hívva (pl. shutdown hook által)
            addFunction(
                FunSpec.builder(ScopeInternal::close.name)
                    .addModifiers(OVERRIDE, SUSPEND)
                    .apply {
                        if (scopeCodeGenerationModel.resolvedScopeModel.components.any { it.value.componentModel.lifecycleModel.terminationFunction != null }) {
                            scopeCodeGenerationModel.componentVariableMap.toList().asReversed().forEach {
                                val componentId = it.first
                                val componentModel =
                                    scopeCodeGenerationModel.resolvedScopeModel.components.getValue(componentId)
                                val terminationFunction =
                                    componentModel.componentModel.lifecycleModel.terminationFunction
                                if (terminationFunction != null) {
                                    addCode(
                                        // TODO try-catch
                                        CodeBlock.builder()
                                            .addStatement(
                                                it.second + ".%N()",
                                                terminationFunction.simpleName.asString()
                                            )
                                            .build()
                                    )
                                        .build()
                                }
                            }
                        }
                    }
                    .build()
            )
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
                    addProperty(
                        PropertySpec.builder(
                            "parentScope",
                            scopeCodeGenerationModel.parentScopeCodeGenerationModel.implementationClassName
                        )
                            .initializer("parentScope") // TODO konstansba kitenni
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
                        CodeBlock.builder().apply { generateStartMethod() }.build()
                    )
                    .build()
            )
            .apply {
                generateCloseMethod()
            }
            .addFunctions(
                resolvedScopeModel.componentQueries
                    .filter { it.staticModel.isFunction }
                    .map {
                        val functionDeclaration = it.staticModel.declaration as KSFunctionDeclaration
                        FunSpec.builder(functionDeclaration.simpleName.asString())
                            .addModifiers(OVERRIDE)
                            .returns(functionDeclaration.returnType!!.toTypeName())
                            .addStatement(
                                "return " +
                                        generateComponentConstructorArgument(
                                            it.resolveDependencies(),
                                            it.dependencyKind
                                        )
                            )
                            .build()
                    }
            )
            .addProperties(
                // TODO ez nagyon hasonló az előzőhöz
                resolvedScopeModel.componentQueries
                    .filter { it.staticModel.isProperty }
                    .map {
                        val propertyDeclaration = it.staticModel.declaration as KSPropertyDeclaration
                        PropertySpec.builder(
                            propertyDeclaration.simpleName.asString(),
                            propertyDeclaration.type.toTypeName()
                        )
                            .addModifiers(OVERRIDE)
                            .getter(
                                FunSpec.getterBuilder()
                                    .addStatement(
                                        "return " +
                                                generateComponentConstructorArgument(
                                                    it.resolveDependencies(),
                                                    it.dependencyKind
                                                )
                                    )
                                    .build()
                            )
                            .build()
                    }
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
            val componentAnnotation = inlineComponentDeclaration.getAnnotationsOfType<Component>().first()
            val componentType = inlineComponentDeclaration.returnType!!.resolve()
            val componentTypeDeclaration =
                componentType.declaration as KSClassDeclaration // TODO ezt valahol ellenőrizni
            InlineComponentModel(
                ComponentId(moduleId, inlineComponentDeclaration.simpleName.asString()),
                componentType,
                inlineComponentDeclaration.parameters.associate {
                    it.name!!.asString() to it.type.resolve().toComponentLookup()
                },
                inlineComponentDeclaration,
                ComponentLifecycleModel( // TODO ezt csak inline-nál lehessen megadni
                    (componentAnnotation.getArgumentValueOrNull(Component::onConstruction.name) as? String)?.let { methodName ->
                        findComponentLifeCycleMethod(
                            inlineComponentDeclaration,
                            componentTypeDeclaration,
                            methodName
                        )
                    },
                    (componentAnnotation.getArgumentValueOrNull(Component::onTerminate.name) as? String)?.let { methodName ->
                        findComponentLifeCycleMethod(
                            inlineComponentDeclaration,
                            componentTypeDeclaration,
                            methodName
                        )
                    }
                )
            )
        } else {
            kspLogger.error("Failed to resolve component type.", inlineComponentDeclaration)
            null
        }

    private fun findComponentLifeCycleMethod(
        inlineComponentDeclaration: KSFunctionDeclaration,
        componentTypeDeclaration: KSClassDeclaration,
        methodName: String
    ) =
        if (methodName.isNotEmpty())
            componentTypeDeclaration.getAllFunctions()
                .firstOrNull { it.simpleName.asString() == methodName }
                .also {
                    if (it == null) {
                        kspLogger.error(
                            "Lifecycle method '$methodName' not found in component implementation `${componentTypeDeclaration.toClassName()}`.",
                            inlineComponentDeclaration
                        )
                    }
                }
        else
            null

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

    // TODO lásd kicsit fentebb, ez így már totál káosz
    private fun ResolvedComponentQueryModel.resolveDependencies(): List<ComponentId> {
        return candidates.map { it.component.id }
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
                .associateBy { it.componentModel.id },
            componentQueries.map {
                ResolvedComponentQueryModel(
                    it,
                    it.componentLookup.dependencyKind,
                    resolve(
                        scopeModules + (parentScopeModel?.modules?.values ?: emptyList()),
                        it.componentLookup
                    ) // TODO kicsit feljebb majdnem ugyanez van
                )
            }
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
