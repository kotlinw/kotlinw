package xyz.kotlinw.di.processor

import com.squareup.kotlinpoet.ClassName
import kotlinw.graph.model.DirectedGraph
import kotlinw.graph.model.Vertex
import xyz.kotlinw.di.api.internal.ComponentId
import xyz.kotlinw.di.api.internal.ModuleId

data class ContainerCodeGenerationModel(
    val interfaceName: ClassName,
    val implementationName: ClassName,
    val scopes: Map<ScopeId, ScopeCodeGenerationModel>
)

data class ScopeCodeGenerationModel(
    val resolvedScopeModel: ResolvedScopeModel,
    val parentScopeCodeGenerationModel: ScopeCodeGenerationModel?,
    val moduleVariableMap: Map<ModuleId, String>,
    val componentGraph: DirectedGraph<ComponentId, Vertex<ComponentId>>,
    val componentVariableMap: Map<ComponentId, String>
)
