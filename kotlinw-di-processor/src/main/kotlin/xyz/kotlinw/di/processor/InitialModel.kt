package xyz.kotlinw.di.processor

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import xyz.kotlinw.di.api.internal.ComponentDependencyKind
import xyz.kotlinw.di.api.internal.ComponentId

data class ContainerModel(val id: String, val scopes: List<ScopeModel>, val modules: List<ModuleModel>)

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
}

data class InlineComponentModel(
    override val id: ComponentId,
    override val componentType: KSType,
    override val dependencyDefinitions: Map<String, ComponentLookup>,
    val factoryMethodName: String
) : ComponentModel

data class ComponentClassModel(
    override val id: ComponentId,
    override val componentType: KSType,
    override val dependencyDefinitions: Map<String, ComponentLookup>,
    val componentClassDeclaration: KSClassDeclaration
) : ComponentModel

data class ComponentLookup(val type: KSType, val dependencyKind: ComponentDependencyKind)

data class ModuleReference(val moduleDeclaration: KSClassDeclaration, val moduleId: String)

data class ScopeModel(
    val parentScopeName: String?,
    val name: ScopeId,
    val scopeInterfaceDeclaration: KSClassDeclaration,
    val declaredModules: List<ModuleReference>,
    val allModules: Set<ModuleReference>
)

typealias ScopeId = String
