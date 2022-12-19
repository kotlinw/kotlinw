package kotlinw.util.stdlib

import kotlin.reflect.KClass

actual val KClass<*>.debugName: String get() = qualifiedName ?: "<unknown class>"

fun KClass<*>.noArgsConstructor() = java.declaredConstructors.first { it.parameterCount == 0 }

inline fun <reified T : Any> KClass<T>.newInstance() =
    noArgsConstructor().also { it.trySetAccessible() }.newInstance() as T
