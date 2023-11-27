package xyz.kotlinw.di.api.internal

typealias ModuleId = String

typealias LocalComponentId = String

data class ComponentId(val moduleId: ModuleId, val localComponentId: LocalComponentId) {

    override fun toString() = "$moduleId/$localComponentId"
}
