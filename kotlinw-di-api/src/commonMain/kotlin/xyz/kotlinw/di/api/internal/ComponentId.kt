package xyz.kotlinw.di.api.internal

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlinw.util.stdlib.debugName

typealias ModuleId = String

typealias LocalComponentId = String

data class ComponentId(val moduleId: ModuleId, val localComponentId: LocalComponentId) {

    override fun toString() = "$moduleId/$localComponentId"
}

// TODO ez implementation detail kellene legyen
fun toComponentId(moduleClass: KClass<*>, inlineComponentFactoryFunction: KFunction<*>): ComponentId =
    ComponentId(moduleClass.debugName, inlineComponentFactoryFunction.name)
