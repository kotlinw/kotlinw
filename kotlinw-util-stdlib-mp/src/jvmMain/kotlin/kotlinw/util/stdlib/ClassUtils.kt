package kotlinw.util.stdlib

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import java.lang.reflect.Constructor
import java.nio.charset.Charset
import kotlin.reflect.KClass

actual val KClass<*>.debugName: String get() = qualifiedName ?: simpleName ?: "<unknown class>"

fun KClass<*>.noArgsConstructor(): Constructor<*> = java.declaredConstructors.first { it.parameterCount == 0 }

inline fun <reified T : Any> KClass<T>.newInstance(): T =
    noArgsConstructor().also { it.trySetAccessible() }.newInstance() as T

sealed interface ReadResourceError {

    object ResourceNotFound : ReadResourceError

    data class ResourceReadFailed(val cause: Throwable) : ReadResourceError
}

fun KClass<*>.readResourceText(name: String, charset: Charset = Charsets.UTF_8): Result<String, ReadResourceError> {
    val url = this.java.getResource(name)
    return if (url != null) {
        runCatching { url.readText(charset) }.mapError { ReadResourceError.ResourceReadFailed(it) }
    } else {
        Err(ReadResourceError.ResourceNotFound)
    }
}
