package xyz.kotlinw.di.processor

import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import kotlinw.ksp.util.isSuspend
import xyz.kotlinw.di.api.internal.ComponentDependencyKind
import xyz.kotlinw.di.api.internal.ComponentId
import xyz.kotlinw.di.api.internal.ModuleId

data class ResolvedContainerModel(
    val containerModel: ContainerModel,
    val scopes: Map<ScopeId, ResolvedScopeModel>
)

data class ResolvedComponentModel(
    val componentModel: ComponentModel,
    val scopeName: ScopeId,
    val dependencyCandidates: Map<String, ResolvedComponentDependencyModel>
)

sealed interface ResolvedDependencyModel {
    val dependencyType: KSType
    val dependencyKind: ComponentDependencyKind
    val candidates: List<ComponentDependencyCandidate>
}

data class ResolvedComponentDependencyModel(
    val dependencyName: String,
    override val dependencyType: KSType,
    override val dependencyKind: ComponentDependencyKind,
    override val candidates: List<ComponentDependencyCandidate>
): ResolvedDependencyModel

data class ResolvedComponentQueryModel(
    val staticModel: ComponentQueryModel,
    val type: KSType,
    override val dependencyKind: ComponentDependencyKind,
    override val candidates: List<ComponentDependencyCandidate>
): ResolvedDependencyModel {

    override val dependencyType: KSType = staticModel.functionDeclaration.returnType!!.resolve()
}

data class ComponentDependencyCandidate(val declaringModule: ModuleModel, val component: ComponentModel) {

    override fun toString() = "ComponentDependencyCandidate(component.id=${component.id})"
}

data class ResolvedScopeModel(
    val scopeModel: ScopeModel,
    val parentScopeModel: ResolvedScopeModel?,
    val modules: Map<ModuleId, ResolvedModuleModel>,
    val components: Map<ComponentId, ResolvedComponentModel>,
    val componentQueries: List<ResolvedComponentQueryModel>
)

data class ResolvedModuleModel(
    val moduleModel: ModuleModel,
    val scopeName: ScopeId
)
