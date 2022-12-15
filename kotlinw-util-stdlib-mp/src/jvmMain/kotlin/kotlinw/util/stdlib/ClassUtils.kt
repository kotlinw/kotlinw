package kotlinw.util.stdlib

import kotlin.reflect.KClass

actual val KClass<*>.debugName: String get() = qualifiedName ?: "<unknown class>"
