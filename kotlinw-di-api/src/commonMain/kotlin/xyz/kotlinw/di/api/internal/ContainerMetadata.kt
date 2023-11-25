package xyz.kotlinw.di.api.internal

import kotlin.reflect.KClass

data class ContainerMetadata(val availableModules: List<ModuleMetadata>)

data class ModuleMetadata(
    val moduleId: String,
    val moduleKClass: KClass<out Any>,
    val components: List<ComponentMetadata<*>>
)

data class ComponentMetadata<T : Any>(
    val moduleLocalId: String,
    val componentConstructor: (List<Any?>) -> T,
    val dependencyCandidates: List<ComponentDependencyCandidateMetadata>,
    val postConstruct: (suspend (T) -> Unit)?,
    val preDestroy: (suspend (T) -> Unit)?
)

enum class ComponentDependencyKind(val isMultiple: Boolean, val isRequired: Boolean) {
    SINGLE_OPTIONAL(false, false),
    SINGLE_REQUIRED(false, true),
    MULTIPLE_OPTIONAL(true, false),
    MULTIPLE_REQUIRED(true, true)
}

data class ComponentDependencyCandidateMetadata(val dependencyKind: ComponentDependencyKind, val candidates: List<ComponentId>)
