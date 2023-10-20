@file:JvmName("ContainerUtilJvm")

package xyz.kotlinw.koin.container

import arrow.atomic.AtomicInt
import kotlin.jvm.JvmName
import kotlinw.util.stdlib.sortedByPriority
import org.koin.core.Koin
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope

/**
 * Copy of constant because it is `private` originally.
 * Source: koin-core/commonMain/org/koin/core/registry/ScopeRegistry.kt:108
 */
const val KOIN_ROOT_SCOPE_ID = "_root_"

inline fun <reified T : Any> Scope.getAllSortedByPriority(): List<T> =
    getAll<T>(T::class).sortedByPriority()

inline fun <reified T : Any> Koin.getAllSortedByPriority(): List<T> =
    getAll<T>().sortedByPriority()

// TODO context(Scope)
fun <T : Any> T.registerStartupTask(scope: Scope, onStartupTask: OnStartupTask<T>): T {
    scope.get<ContainerStartupCoordinator>().registerOnStartupTask(this, onStartupTask)
    return this
}

// TODO context(Scope)
fun <T : Any> T.registerShutdownTask(scope: Scope, onShutdownTask: OnShutdownTask<T>): T {
    scope.get<ContainerShutdownCoordinator>().registerOnShutdownTask(this, onShutdownTask)
    return this
}

private object AnonymousStartupTaskRegistrant

private val nextId = AtomicInt(0)

// TODO context
fun Module.uniqueNamed(debugName: String) = named("$debugName-${nextId.incrementAndGet()}")

fun Module.registerStartupTask(debugName: String, startupTaskProvider: Scope.() -> OnStartupTask<Unit>) {
    single(uniqueNamed(debugName), createdAtStart = true) {
        val startupTask = startupTaskProvider()
        AnonymousStartupTaskRegistrant.registerStartupTask(this) {
            startupTask(Unit)
        }
    }
}

fun Module.onModuleReady(startupTaskProvider: Scope.() -> OnStartupTask<Unit>) =
    registerStartupTask("onModuleReady-$id", startupTaskProvider)
