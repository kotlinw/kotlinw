package xyz.kotlinw.di.processor

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import xyz.kotlinw.di.api.internal.ComponentDependencyKind
import xyz.kotlinw.di.api.internal.ComponentId
import xyz.kotlinw.di.api.OnConstruction
import xyz.kotlinw.di.api.OnTerminate

data class ContainerModel(
    val id: String,
    val scopes: List<ScopeModel>,
    val modules: List<ModuleModel>,
    val declaration: KSClassDeclaration
)

data class ModuleModel(
    val id: String,
    val declaringClass: KSClassDeclaration,
    val components: List<ComponentModel>,
    val componentScanPackageName: String?
)

sealed interface ComponentModel {
    val id: ComponentId
    val componentType: KSType
    val dependencyDefinitions: Map<String, ComponentLookup>
    val lifecycleModel: ComponentLifecycleModel
}

data class ComponentLifecycleModel(

    /**
     * Component function annotated with [OnConstruction] to be executed during construction.
     */
    val constructionFunction: KSFunctionDeclaration?,

    /**
     * Component function annotated with [OnTerminate] to be executed during component termination.
     */
    val terminationFunction: KSFunctionDeclaration?
)

data class InlineComponentModel(
    override val id: ComponentId,
    override val componentType: KSType,
    override val dependencyDefinitions: Map<String, ComponentLookup>,
    val factoryMethod: KSFunctionDeclaration,
    override val lifecycleModel: ComponentLifecycleModel
) : ComponentModel {

    val factoryMethodName: String get() = factoryMethod.simpleName.asString()
}

data class ComponentClassModel(
    override val id: ComponentId,
    override val componentType: KSType,
    override val dependencyDefinitions: Map<String, ComponentLookup>,
    val componentClassDeclaration: KSClassDeclaration,
    override val lifecycleModel: ComponentLifecycleModel
) : ComponentModel

data class ComponentLookup(val type: KSType, val dependencyKind: ComponentDependencyKind)

data class ModuleReference(val moduleDeclaration: KSClassDeclaration, val moduleId: String)

data class ScopeModel(
    val parentScopeName: String?,
    val name: ScopeId,
    val scopeDeclarationFunction: KSFunctionDeclaration,
    val scopeInterfaceDeclaration: KSClassDeclaration,
    val declaredModules: List<ModuleReference>,
    val allModules: Set<ModuleReference>
)

typealias ScopeId = String
