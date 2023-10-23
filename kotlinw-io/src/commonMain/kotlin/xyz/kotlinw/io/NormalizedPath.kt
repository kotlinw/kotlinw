package xyz.kotlinw.io

import kotlin.jvm.JvmInline
import kotlinx.io.files.Path
import kotlinx.io.files.SystemPathSeparator
import kotlinx.serialization.Serializable
import xyz.kotlinw.io.NormalizedPath.Companion.ROOT_PATH_STRING

@Serializable
sealed interface NormalizedPath {

    companion object {

        const val PATH_SEPARATOR_CHAR = '/'

        const val ROOT_PATH_STRING = ""
    }

    val value: String

    val parent: NormalizedPath?
}

val NormalizedPath.lastSegment
    get() =
        if (value.contains(NormalizedPath.PATH_SEPARATOR_CHAR))
            value.substring(value.lastIndexOf(NormalizedPath.PATH_SEPARATOR_CHAR))
        else
            value

private fun validatePath(path: String) {
    require(!path.endsWith(NormalizedPath.PATH_SEPARATOR_CHAR))
    require(!path.contains("//"))
}

@JvmInline
@Serializable
value class AbsolutePath(override val value: String) : NormalizedPath {

    init {
        validatePath(value)
    }

    override val parent: AbsolutePath? get() = parentPathValue()?.let { AbsolutePath(it) }

    override fun toString() = AbsolutePath::class.simpleName!! + "(" + value + ")"
}

@JvmInline
@Serializable
value class RelativePath(override val value: String) : NormalizedPath {

    init {
        validatePath(value)
    }

    override val parent: RelativePath? get() = parentPathValue()?.let { RelativePath(it) }

    override fun toString() = RelativePath::class.simpleName!! + "(" + value + ")"
}

fun AbsolutePath.append(relativePath: RelativePath) =
    AbsolutePath(value + NormalizedPath.PATH_SEPARATOR_CHAR + relativePath.value)

fun RelativePath.append(relativePath: RelativePath) =
    RelativePath(value + NormalizedPath.PATH_SEPARATOR_CHAR + relativePath.value)

fun NormalizedPath.append(relativePath: RelativePath) =
    when (this) {
        is AbsolutePath -> append(relativePath)
        is RelativePath -> append(relativePath)
    }

fun NormalizedPath.toFileSystemPath() = Path(value.replace(NormalizedPath.PATH_SEPARATOR_CHAR, SystemPathSeparator))

fun Path.toNormalizedPath() = if (isAbsolute) AbsolutePath(toString()) else RelativePath(toString())

private fun NormalizedPath.isAncestorOfImpl(path: NormalizedPath): Boolean =
    path.value.startsWith(value + NormalizedPath.PATH_SEPARATOR_CHAR)

fun AbsolutePath.isAncestorOf(path: NormalizedPath): Boolean = isAncestorOfImpl(path)

fun RelativePath.isAncestorOf(path: RelativePath): Boolean = isAncestorOfImpl(path)

private fun NormalizedPath.isDescendantOfImpl(path: NormalizedPath): Boolean =
    value.startsWith(path.value + NormalizedPath.PATH_SEPARATOR_CHAR)

fun AbsolutePath.isDescendantOf(path: AbsolutePath): Boolean = isDescendantOfImpl(path)

fun RelativePath.isDescendantOf(path: RelativePath): Boolean = isDescendantOfImpl(path)

fun AbsolutePath.asRelativeTo(basePath: AbsolutePath): RelativePath {
    require(isDescendantOf(basePath))
    return RelativePath(value.substring(basePath.value.length + 1))
}

fun RelativePath.asRelativeTo(basePath: RelativePath): RelativePath {
    require(isDescendantOf(basePath))
    return RelativePath(value.substring(basePath.value.length + 1))
}

private fun NormalizedPath.parentPathValue(): String? =
    if (value.contains(NormalizedPath.PATH_SEPARATOR_CHAR))
        value.substring(0, value.lastIndexOf(NormalizedPath.PATH_SEPARATOR_CHAR))
    else if (value == ROOT_PATH_STRING)
        null
    else
        ROOT_PATH_STRING
