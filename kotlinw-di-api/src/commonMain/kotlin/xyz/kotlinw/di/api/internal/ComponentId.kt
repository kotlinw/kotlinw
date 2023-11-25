package xyz.kotlinw.di.api.internal

data class ComponentId(val moduleId: String, val moduleLocalId: String) {

    override fun toString() = "$moduleId/$moduleLocalId"
}

