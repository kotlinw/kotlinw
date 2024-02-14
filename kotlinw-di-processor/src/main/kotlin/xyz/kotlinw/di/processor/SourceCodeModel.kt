package xyz.kotlinw.di.processor

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import xyz.kotlinw.di.api.OnConstruction
import xyz.kotlinw.di.api.OnTerminate
import xyz.kotlinw.di.api.internal.ComponentDependencyKind
import xyz.kotlinw.di.api.internal.ComponentId

data class ContainerModel(
    val id: String,
    val scopes: List<ScopeModel>,
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
    val qualifier: String?
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
    override val qualifier: String?,
    override val dependencyDefinitions: Map<String, ComponentLookup>,
    val factoryMethod: KSFunctionDeclaration,
    override val lifecycleModel: ComponentLifecycleModel
) : ComponentModel {

    val factoryMethodName: String get() = factoryMethod.simpleName.asString()
}

data class ComponentClassModel(
    override val id: ComponentId,
    override val componentType: KSType,
    override val qualifier: String?,
    override val dependencyDefinitions: Map<String, ComponentLookup>,
    val componentClassDeclaration: KSClassDeclaration,
    override val lifecycleModel: ComponentLifecycleModel
) : ComponentModel

private const val EXTERNAL_MODULE_ID = "<external>"

data class ExternalComponentModel(
    val name: String,
    override val componentType: KSType,
    override val qualifier: String?
) : ComponentModel {

    override val id: ComponentId get() = ComponentId(EXTERNAL_MODULE_ID, name)

    override val dependencyDefinitions: Map<String, ComponentLookup> get() = emptyMap()

    override val lifecycleModel: ComponentLifecycleModel get() = ComponentLifecycleModel(null, null)
}

data class ComponentLookup(
    val type: KSType,
    val qualifier: String?,
    val dependencyKind: ComponentDependencyKind,
    val kspMessageTarget: KSNode
)

data class ModuleReference(val moduleDeclaration: KSClassDeclaration, val moduleId: String)

data class ScopeModel(
    val parentScopeName: String?,
    val name: ScopeId,
    val scopeDeclarationFunction: KSFunctionDeclaration,
    val scopeInterfaceDeclaration: KSClassDeclaration,
    val declaredModules: List<ModuleReference>,
    val allModules: Set<ModuleModel>,
    val componentQueries: List<ComponentQueryModel>,
    val externalComponents: List<ExternalComponentModel>,
    val ignoredComponents: Set<ComponentId>
)

data class ComponentQueryModel(
    val declaration: KSDeclaration,
    val type: KSType,
    val componentLookup: ComponentLookup
) {

    val isProperty get() = declaration is KSPropertyDeclaration

    val isFunction get() = declaration is KSFunctionDeclaration
}

typealias ScopeId = String
