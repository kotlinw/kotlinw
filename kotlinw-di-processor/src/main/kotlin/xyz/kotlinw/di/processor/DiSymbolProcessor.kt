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
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.INTERNAL
import com.squareup.kotlinpoet.KModifier.LATEINIT
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.KModifier.SUSPEND
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import kotlinw.graph.algorithm.AcyclicCheckResult.Cyclic
import kotlinw.graph.algorithm.checkAcyclic
import kotlinw.graph.algorithm.reverseTopologicalSort
import kotlinw.graph.model.DirectedGraph
import kotlinw.graph.model.Vertex
import kotlinw.graph.model.build
import kotlinw.ksp.util.companionObjectOrNull
import kotlinw.ksp.util.getAnnotationsOfType
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
import xyz.kotlinw.di.api.Qualifier
import xyz.kotlinw.di.api.Scope
import xyz.kotlinw.di.api.internal.ComponentDependencyKind
import xyz.kotlinw.di.api.internal.ComponentDependencyKind.MULTIPLE_OPTIONAL
import xyz.kotlinw.di.api.internal.ComponentDependencyKind.MULTIPLE_REQUIRED
import xyz.kotlinw.di.api.internal.ComponentDependencyKind.SINGLE_OPTIONAL
import xyz.kotlinw.di.api.internal.ComponentDependencyKind.SINGLE_REQUIRED
import xyz.kotlinw.di.api.internal.ComponentId
import xyz.kotlinw.di.api.internal.ScopeInternal

private class StopKspProcessingException(override val message: String) : RuntimeException()

private class DelayKspProcessingException() : RuntimeException()

// TODO ha egy nem többértékű függőségből 1+ példány elérhető, akkor adjon hibát

// TODO ha egy inline component típusa egy generált class, ami még nem lett legenerálva, akkor
// [ksp] java.lang.IllegalArgumentException: Error type '<ERROR TYPE>' is not resolvable in the current round of processing.
// hiba jelentkezik, pl. ennél:
//     @Component
//    fun applicationPersistentClassProvider() = GeneratedPackagePersistentClassProvider()

// TODO legyen a scope interfészben lehetőség arra, hogy bizonyos komponenseket felírjunk, pl. egy external component lehetne @ExternalComponent(override = true)
// TODO lehessen constructor reference-szel komponenst definiálni: @Component(type = A::class) fun a() = ::AImpl
// TODO körkörös referencia kezelése @Module.includeModules-ben
// TODO warning, ha egy modul többször van felsorolva
// TODO a container implementációt generálja le akkor is, ha egyébként vannak hibák, hogy a container-t és a scope-okat létrehozó kód még helyes maradjon
// TODO attól, hogy csak container-t tartalmazó Gradle modul esetén generál kódot, ellenőrizni még kellene az adott Gradle modulban deklarált elemeket
// TODO ellenőrizni, hogy @Container interfészben minden metódusnak @Scope-pal annotatáltnak kell lennie
// TODO ellenőrzés, hogy a scope interfészben minden metódus annotatált-e @ComponentQuery-vel
// FIXME hiányzó dependency esetén hibás kódot generál, és nem ad hasznos fordítási hibát
// TODO component query-nél ellenőrizni kellene a metódus elején, hogy el a scope-on meg lett-e már hívva a start()
// TODO ha az includeModules-ben @Module-lal nem annotált class szerepel, akkor elszáll
// TODO írja ki, hogy pontosan melyik komponens hiányolja: [ksp] Exactly 1 component instance of type `com.erinors.workflow.api.WorkflowDefinitionLookup` expected but found 0:

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

// TODO warning, ha interfész típus elrejt a komponens típusa, pl. WorkflowManager a típus, de a WorkflowManagerImpl implementálja a ContainerLifecycleListener-t is

@OptIn(KspExperimental::class)
class DiSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val kspLogger: KSPLogger,
    private val kspOptions: Map<String, String>
) : SymbolProcessor {

    private val containerCreateFunctionsAlreadyGenerated = mutableSetOf<ClassName>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
//        val componentDeclarations = resolver.getSymbolsWithAnnotation(Component::class.qualifiedName!!).toList()
//        val moduleDeclarations = resolver.getSymbolsWithAnnotation(Module::class.qualifiedName!!).toList()
        val containerDeclarations = resolver.getSymbolsWithAnnotation(Container::class.qualifiedName!!).toList()

        val processableContainerDeclarations = mutableSetOf<KSAnnotated>()

        containerDeclarations
            .forEach { containerDeclaration ->
                if (containerDeclaration is KSClassDeclaration) {
                    if (containerDeclaration.toClassName() !in containerCreateFunctionsAlreadyGenerated) {
                        if (containerDeclaration.hasCompanionObject) {
                            generateContainerCreateFunction(containerDeclaration)
                            containerCreateFunctionsAlreadyGenerated.add(containerDeclaration.toClassName())
                            processableContainerDeclarations.add(containerDeclaration)
                        } else {
                            kspLogger.error(
                                "Container declaration interface should have a `companion object` (see related Kotlin feature request: https://youtrack.jetbrains.com/issue/KT-11968/Research-and-prototype-namespace-based-solution-for-statics-and-static-extensions ).",
                                containerDeclaration
                            )
                        }
                    } else {
                        // create() extension function has been generated in a previous round
                        processableContainerDeclarations.add(containerDeclaration)
                    }
                } else {
                    kspLogger.error("Container declaration should be an `interface`.", containerDeclaration)
                }
            }

        val validContainerDeclarations = processableContainerDeclarations.filter { it.validate() }

        val failedProcessings = mutableListOf<KSAnnotated>()
        try {
            validContainerDeclarations.forEach {
                try {
                    processContainerDeclaration(it, resolver)
                } catch (e: DelayKspProcessingException) {
                    failedProcessings.add(it)
                }
            }
        } catch (e: StopKspProcessingException) {
            kspLogger.error(e.message)
        }

        return containerDeclarations - validContainerDeclarations + failedProcessings
    }

    private fun generateContainerCreateFunction(containerDeclaration: KSClassDeclaration) {
        FileSpec
            .builder(containerDeclaration.packageName.asString(), containerDeclaration.simpleName.asString() + "Util")
            .addFunction(
                FunSpec
                    .builder("create")
                    .receiver(containerDeclaration.companionObjectOrNull!!.toClassName())
                    .returns(containerDeclaration.toClassName())
                    .addStatement("return %T()", containerImplementationName(containerDeclaration.toClassName()))
                    .build()
            )
            .build()
            .writeTo(codeGenerator, true)
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
                                        val declaredModules =
                                            scopeAnnotation.getArgumentValueOrNull<List<KSType>>("modules")
                                                ?: emptyList()
                                        val parentScopeName =
                                            scopeAnnotation.getArgumentValueOrNull<String>("parent")
                                                ?.let { it.ifEmpty { null } }

                                        if (declaredModules.all {
                                                it.declaration is KSClassDeclaration
                                                        && it.declaration.isAnnotationPresent(Module::class)
                                            }
                                        ) {
                                            val ignoredComponents =
                                                scopeAnnotation
                                                    .getArgumentValueOrNull<List<KSAnnotation>>("ignoredComponents")
                                                    ?.map {
                                                        ComponentId(
                                                            it.getArgumentValueOrNull<KSType>("module")!!.getModuleId(),
                                                            it.getArgumentValueOrNull<String>("localComponentId")!!
                                                        )
                                                    }
                                                    ?.toSet()
                                                    ?: emptySet()

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
                                                    .mapNotNull {
                                                        processModuleReference(it, resolver, ignoredComponents)
                                                    }
                                                    .toSet(),
                                                collectComponentQueries(scopeInterfaceDeclaration),
                                                scopeDeclarationFunction
                                                    .parameters
                                                    .drop(if (parentScopeName != null) 1 else 0)
                                                    .map {
                                                        ExternalComponentModel(
                                                            it.name!!.asString(),
                                                            it.type.resolve(),
                                                            it.type.extractQualifierOrNull()
                                                        )
                                                    },
                                                ignoredComponents
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

// TODO check overlapping modules
//                    val overlappingModules = mutableListOf<Pair<ModuleModel, ModuleModel>>()
//                    moduleDeclarations.forEach { currentModule ->
//                        moduleDeclarations.forEach { referenceModule ->
//                            if (currentModule != referenceModule
//                                && currentModule.componentScanPackageName != null
//                                && referenceModule.componentScanPackageName != null
//                                && !overlappingModules.contains(referenceModule to currentModule)
//                                && !overlappingModules.contains(currentModule to referenceModule)
//                                && (currentModule.componentScanPackageName.startsWith(referenceModule.componentScanPackageName))
//                            ) {
//                                overlappingModules.add(currentModule to referenceModule)
//                            }
//                        }
//                    }
//
//                    if (overlappingModules.isEmpty()) {
                    ContainerModel(
                        containerDeclaration.qualifiedName!!.asString(),
                        scopeDeclarations,
                        containerDeclaration
                    )
//                    } else {
//                        overlappingModules.forEach {
//                            kspLogger.error("Overlapping modules with @${ComponentScan::class.simpleName}: ${it.first.id} and ${it.second.id}")
//                        }
//                        null
//                    }
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
                        .addModifiers(INTERNAL) // TODO jelezni, hogy nem része az API-nak
                        .addFunctions(
                            codeGenerationModel.scopes.values.map { scopeCodeGenerationModel ->
                                generateScopeBuilderFunction(scopeCodeGenerationModel)
                            }
                        )
                        .build()
                )
                .build()
                .writeTo(codeGenerator, true)
        }
    }

    private fun collectComponentQueries(scopeInterfaceDeclaration: KSClassDeclaration): List<ComponentQueryModel> {
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
                ComponentQueryModel(
                    it,
                    type,
                    createComponentLookup(type, it.returnType!!.extractQualifierOrNull(), it, null)
                )
            }
            .toList() +
                componentQueryProperties
                    .map {
                        val type = it.type.resolve()
                        ComponentQueryModel(
                            it,
                            type,
                            createComponentLookup(type, it.type.extractQualifierOrNull(), it, null)
                        )
                    }
                    .toList()
    }

    private fun createCodeGenerationModel(resolvedContainerModel: ResolvedContainerModel): ContainerCodeGenerationModel {
        val containerInterfaceName = resolvedContainerModel.containerModel.declaration.toClassName()
        val containerImplementationPackageName = containerInterfaceName.packageName

        val scopes = mutableMapOf<ScopeId, ScopeCodeGenerationModel>()
        resolvedContainerModel.scopes.forEach { (scopeId, resolvedScopeModel) ->
            val componentGraph = buildComponentGraph(resolvedScopeModel)

            componentGraph.checkAcyclic().also {
                if (it is Cyclic) {
                    throw StopKspProcessingException("Dependency cycle detected: ${it.path.joinToString(" -> ") { it.data.toString() }}")
                }
            }

            val reverseTopologicalSort = componentGraph.reverseTopologicalSort().toList()

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
                reverseTopologicalSort
                    .filter { it.data in resolvedScopeModel.components }
                    .mapIndexed { index, componentVertex ->
                        componentVertex.data to "c$index"
                    }// TODO beszédes neveket, hogy a generált kód olvashatóbb legyen
                    .toMap()
            )
        }

        return ContainerCodeGenerationModel(
            containerInterfaceName,
            containerImplementationName(containerInterfaceName),
            scopes
        )
    }

    private fun containerImplementationName(
        containerInterfaceName: ClassName
    ) = ClassName(
        containerInterfaceName.packageName,
        containerInterfaceName.simpleName + "Impl"
    )

    private fun generateScopeBuilderFunction(scopeCodeGenerationModel: ScopeCodeGenerationModel) =
        FunSpec
            .builder(scopeCodeGenerationModel.resolvedScopeModel.scopeModel.scopeDeclarationFunction.simpleName.asString())
            .addModifiers(OVERRIDE)
            .returns(scopeCodeGenerationModel.resolvedScopeModel.scopeModel.scopeInterfaceDeclaration.toClassName())
            .apply {
                addParameters(
                    scopeCodeGenerationModel.resolvedScopeModel.scopeModel.scopeDeclarationFunction.parameters.map {
                        ParameterSpec(it.name!!.asString(), it.type.toTypeName())
                    }
                )

                val externalComponentParameterNames =
                    scopeCodeGenerationModel.resolvedScopeModel.scopeModel.externalComponents.joinToString { it.name }

                if (scopeCodeGenerationModel.parentScopeCodeGenerationModel == null) {
                    addStatement(
                        "return %T($externalComponentParameterNames)",
                        scopeCodeGenerationModel.implementationClassName
                    )
                } else {
                    addStatement(
                        "return %T(%N as %T, $externalComponentParameterNames)",
                        scopeCodeGenerationModel.implementationClassName,
                        scopeCodeGenerationModel.resolvedScopeModel.scopeModel.scopeDeclarationFunction.parameters.first().name!!.asString(),
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

                if (componentModel.componentModel !is ExternalComponentModel) {
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

                                is ExternalComponentModel -> AssertionError()
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
                                    val variableName = it.second
                                    addCode(
                                        // TODO try-catch
                                        CodeBlock.builder()
                                            .addStatement(
                                                "if (this::%N.%N) %N.%N()",
                                                variableName,
                                                "isInitialized",
                                                variableName,
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
                val hasParentScope = scopeCodeGenerationModel.parentScopeCodeGenerationModel != null
                val hasExternalComponents = resolvedScopeModel.scopeModel.externalComponents.isNotEmpty()
                if (hasParentScope || hasExternalComponents) {
                    primaryConstructor(
                        FunSpec.constructorBuilder()
                            .apply {
                                if (hasParentScope) {
                                    addParameter(
                                        "parentScope",
                                        scopeCodeGenerationModel.parentScopeCodeGenerationModel!!.implementationClassName
                                    )
                                }

                                resolvedScopeModel.scopeModel.externalComponents.forEach {
                                    addParameter(it.name, it.componentType.toTypeName())
                                }
                            }
                            .build()
                    )

                    if (hasParentScope) {
                        addProperty(
                            PropertySpec.builder(
                                "parentScope",
                                scopeCodeGenerationModel.parentScopeCodeGenerationModel!!.implementationClassName
                            )
                                .initializer("parentScope") // TODO konstansba kitenni
                                .build()
                        )
                    }

                    resolvedScopeModel.scopeModel.externalComponents.forEach {
                        addProperty(
                            PropertySpec.builder(
                                it.name,
                                it.componentType.toTypeName()
                            )
                                .initializer(it.name)
                                .build()
                        )
                    }
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
                            try {
                                resolvedScopeModel.components.getValue(componentId).componentModel.componentType.toTypeName()
                            } catch (e: Exception) {
                                kspLogger.warn("wtf: $componentId") // TODO úgy tűnik, hogy a toTypeName() hívásokat kell try-catch-be tenni, mert azok elszállnak, ha bármi miatt "hibás" a type
                                throw DelayKspProcessingException()
                            },
                            LATEINIT
                        )
                            .addKdoc("$componentId")
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

    private fun processModuleReference(
        moduleReference: ModuleReference,
        resolver: Resolver,
        ignoredComponents: Set<ComponentId>
    ): ModuleModel? {
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
                (inlineComponents as List<ComponentModel> + componentClasses as List<ComponentModel>)
                    .filter { it.id !in ignoredComponents },
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
                            componentClassDeclaration.extractQualifierOrNull(),
                            componentClassDeclaration.primaryConstructor!!.parameters.associate {
                                it.name!!.asString() to createComponentLookup(
                                    it.type.resolve(),
                                    it.type.extractQualifierOrNull(),
                                    it,
                                    ComponentId(moduleId, componentClassDeclaration.qualifiedName!!.asString())
                                )
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
                componentType.declaration.asClassDeclaration() // TODO ezt valahol ellenőrizni
            InlineComponentModel(
                ComponentId(moduleId, inlineComponentDeclaration.simpleName.asString()),
                componentType,
                inlineComponentDeclaration.extractQualifierOrNull(),
                inlineComponentDeclaration.parameters.associate {
                    it.name!!.asString() to createComponentLookup(
                        it.type.resolve(),
                        it.type.extractQualifierOrNull(),
                        it,
                        componentTypeDeclaration
                    )
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
                            "Lifecycle method '$methodName' specified by ${inlineComponentDeclaration.qualifiedName?.asString()} not found in component implementation `${componentTypeDeclaration.toClassName()}`.",
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

            // Make sure that the ComponentLifecycleCoordinator instance will be the first component to be created

            // TODO ezt a fix hivatkozást valami szebb megoldással kiváltani
            val lifecycleCoordinatorComponentId =
                ComponentId("xyz.kotlinw.module.core.CoreModule", "containerLifecycleCoordinator")
            if (components.containsKey(lifecycleCoordinatorComponentId)) {
                val lifecycleCoordinatorVertex = vertexes.getValue(lifecycleCoordinatorComponentId)
                components
                    .filter { it.key != lifecycleCoordinatorComponentId }
                    .forEach {
                        val componentId = it.key
                        val componentVertex = vertexes.getValue(componentId)
                        if (!hasEdge(componentVertex, lifecycleCoordinatorVertex)) {
                            edge(componentVertex, lifecycleCoordinatorVertex)
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

            (moduleType.getAnnotationsOfType<Module>()
                .first() // TODO a first() dobott NoSuchElementException-t amikor az egyik tranzitív modul nem volt elérhető, mert hiányzott a hozzá tartozó Gradle dependency
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

    private fun createComponentLookup(
        ksType: KSType,
        qualifier: String?,
        kspMessageTarget: KSNode,
        debugInfo: Any?
    ): ComponentLookup {
        val parameterDeclaration = ksType.declaration.asClassDeclaration()
        return if (parameterDeclaration.qualifiedName == null) {
            // FIXME erre nem itt kellene rádöbbenni; pl. akkor van ilyen, ha
            // - a module osztály @Component metódusának return type hibás
            // - a module osztály @Component metódusának parameter type-ja hibás, pl. nincs megadva a type argument
            // - a scope factory metódus @Component paraméter osztályának típusa hibás
            // - de olyan is volt, hogy a más Gradle modulban definiált komponens függősége egy implementation()-ként szerepelt a Gradle szkriptben, nem api()-ként
            // log: "" + ksType + ", " + parameterDeclaration + ", " + kspMessageTarget + ", " + debugInfo
            throw DelayKspProcessingException()
        } else if (parameterDeclaration.qualifiedName!!.asString() == List::class.qualifiedName) {
            ComponentLookup(
                ksType.arguments[0].type!!.resolve(),
                qualifier,
                MULTIPLE_OPTIONAL,
                kspMessageTarget
            ) // TODO handle MULITPLE_REQUIRED
        } else if (ksType.isMarkedNullable) {
            ComponentLookup(ksType, qualifier, SINGLE_OPTIONAL, kspMessageTarget)
        } else {
            ComponentLookup(ksType, qualifier, SINGLE_REQUIRED, kspMessageTarget)
        }
    }

    private fun resolveContainerModel(containerModel: ContainerModel): ResolvedContainerModel {
        val resolvedScopes = mutableMapOf<String, ResolvedScopeModel>()
        containerModel.scopes
            .sortedWith { o1, o2 ->
                // TODO ezt nem lehet szebben?
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
                        // TODO ennek az első paramétere ne átadva legyen, hanem maga a metódus határozza meg az értékét
                        scope.allModules
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
    ): ResolvedScopeModel {
        val componentDependencyCandidates =
            (scopeModules + (parentScopeModel?.modules?.values ?: emptyList()))
                .flatMap { moduleModel ->
                    moduleModel.moduleModel.components.map { ComponentDependencyCandidate(it) }
                } +
                    externalComponents.map { ComponentDependencyCandidate(it) }
        return ResolvedScopeModel(
            this,
            parentScopeModel,
            scopeModules.associateBy { it.moduleModel.id },
            (
                    scopeModules
                        .flatMap {
                            it.moduleModel.components.map { componentModel ->
                                ResolvedComponentModel(
                                    componentModel,
                                    name,
                                    componentModel.dependencyDefinitions.map {
                                        ResolvedComponentDependencyModel(
                                            it.value,
                                            it.key,
                                            it.value.type,
                                            it.value.dependencyKind,
                                            resolve(
                                                componentDependencyCandidates,
                                                it.value
                                            ) // TODO több szintű parent-et is lekövetni rekurzívan
                                        ).also { resolvedComponentDependencyModel ->
                                            when (resolvedComponentDependencyModel.dependencyKind) {
                                                SINGLE_OPTIONAL ->
                                                    if (resolvedComponentDependencyModel.candidates.size > 1) {
                                                        kspLogger.error(
                                                            "${componentModel.id}: 0 or 1 component instance of type ${resolvedComponentDependencyModel.format()} expected but found ${resolvedComponentDependencyModel.candidates.size}: ${resolvedComponentDependencyModel.candidates.joinToString { it.component.id.toString() }}",
                                                            resolvedComponentDependencyModel.componentLookup.kspMessageTarget
                                                        )
                                                    }

                                                SINGLE_REQUIRED ->
                                                    if (resolvedComponentDependencyModel.candidates.size != 1) {
                                                        kspLogger.error(
                                                            "${componentModel.id}: Exactly 1 component instance of type ${resolvedComponentDependencyModel.format()} expected but found ${resolvedComponentDependencyModel.candidates.size}: ${resolvedComponentDependencyModel.candidates.joinToString { it.component.id.toString() }}",
                                                            resolvedComponentDependencyModel.componentLookup.kspMessageTarget
                                                        )
                                                    }

                                                MULTIPLE_OPTIONAL -> {}

                                                MULTIPLE_REQUIRED ->
                                                    if (resolvedComponentDependencyModel.candidates.isEmpty()) {
                                                        kspLogger.error(
                                                            "${componentModel.id}: 1 or more component instances of type ${resolvedComponentDependencyModel.format()} expected but found ${resolvedComponentDependencyModel.candidates.size}: ${resolvedComponentDependencyModel.candidates.joinToString { it.component.id.toString() }}",
                                                            resolvedComponentDependencyModel.componentLookup.kspMessageTarget
                                                        )
                                                    }
                                            }
                                        }
                                    }.associateBy { it.dependencyName }
                                )
                            }
                        } +
                            externalComponents.map { ResolvedComponentModel(it, name, emptyMap()) }
                    )
                .associateBy { it.componentModel.id },
            componentQueries.map {
                ResolvedComponentQueryModel(
                    it,
                    it.componentLookup.dependencyKind,
                    resolve(
                        componentDependencyCandidates,
                        it.componentLookup
                    ) // TODO kicsit feljebb majdnem ugyanez van
                )
            }
        )
    }

    private fun resolve(
        componentDependencyCandidates: List<ComponentDependencyCandidate>,
        componentLookup: ComponentLookup
    ): List<ComponentDependencyCandidate> = // TODO sokkal hatékonyabbra
        componentDependencyCandidates
            .filter {
                componentLookup.type.isAssignableFrom(it.component.componentType) &&
                        if (componentLookup.qualifier != null) it.component.qualifier == componentLookup.qualifier else true
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

    private fun KSClassDeclaration.getModuleId() = qualifiedName!!.asString() // TODO !!

    private fun KSType.getModuleId() = (declaration as? KSClassDeclaration)?.getModuleId()
        ?: throw IllegalStateException() // TODO rendes KSP hibát adni
}

private fun KSDeclaration.asClassDeclaration(): KSClassDeclaration =
    when (this) {
        is KSClassDeclaration -> this
        is KSTypeAlias -> type.resolve().declaration.asClassDeclaration()
        else -> throw IllegalStateException("$this is not a class declaration.")
    }

private fun ResolvedComponentDependencyModel.format(): String =
    if (componentLookup.qualifier != null)
        "`${dependencyType.toTypeName()}` with qualifier \"${componentLookup.qualifier}\""
    else
        "`${dependencyType.toTypeName()}`"

private fun KSAnnotated.extractQualifierOrNull(): String? =
    buildList {
        getQualifierOrNull()?.also { add(it) }
        addAll(annotations.mapNotNull { it.annotationType.resolve().declaration.getQualifierOrNull() })
    }
        .also {
            if (it.size > 1) {
                throw RuntimeException("Single qualifier expected but found multiple ones: $it") // TODO annotated
            }
        }
        .firstOrNull()


private fun KSAnnotated.getQualifierOrNull(): String? =
    getAnnotationsOfType<Qualifier>().toList().firstOrNull()?.getArgumentValueOrNull(Qualifier::value.name)

private fun ResolvedScopeModel.collectComponents(): Map<ComponentId, ResolvedComponentModel> =
    components + (parentScopeModel?.collectComponents() ?: emptyMap())

private inline fun <reified T : Annotation> annotationDisplayName(): String = "@" + T::class.simpleName
