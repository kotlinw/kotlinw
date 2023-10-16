package xyz.kotlinw.io

import kotlin.jvm.JvmInline
import kotlinx.io.files.Path
import kotlinx.io.files.SystemPathSeparator
import kotlinx.serialization.Serializable

@Serializable
sealed interface NormalizedPath {

    companion object {

        const val PATH_SEPARATOR = '/'

        const val CURRENT_FOLDER_ALIAS = "."

        const val PARENT_FOLDER_ALIAS = ".."
    }

    val value: String
}

val NormalizedPath.lastSegment
    get() =
        if (value.contains(NormalizedPath.PATH_SEPARATOR))
            value.substring(value.lastIndexOf(NormalizedPath.PATH_SEPARATOR))
        else
            value

private fun validatePath(path: String) {
    require(!path.endsWith(NormalizedPath.PATH_SEPARATOR))
    require(!path.contains("//"))
}

@JvmInline
@Serializable
value class AbsolutePath(override val value: String) : NormalizedPath {

    init {
        validatePath(value)
    }

    override fun toString() = AbsolutePath::class.simpleName!! + "(" + value + ")"
}

@JvmInline
@Serializable
value class RelativePath(override val value: String) : NormalizedPath {

    init {
        validatePath(value)
    }

    override fun toString() = RelativePath::class.simpleName!! + "(" + value + ")"
}

fun AbsolutePath.append(relativePath: RelativePath) =
    AbsolutePath(value + NormalizedPath.PATH_SEPARATOR + relativePath.value)

fun RelativePath.append(relativePath: RelativePath) =
    RelativePath(value + NormalizedPath.PATH_SEPARATOR + relativePath.value)

fun NormalizedPath.append(relativePath: RelativePath) =
    when (this) {
        is AbsolutePath -> append(relativePath)
        is RelativePath -> append(relativePath)
    }

fun NormalizedPath.toFileSystemPath() = Path(value.replace(NormalizedPath.PATH_SEPARATOR, SystemPathSeparator))

fun Path.toNormalizedPath() = if (isAbsolute) AbsolutePath(name) else RelativePath(name)

private fun NormalizedPath.isAncestorOfImpl(path: NormalizedPath): Boolean =
    path.value.startsWith(value + NormalizedPath.PATH_SEPARATOR)

fun AbsolutePath.isAncestorOf(path: NormalizedPath): Boolean = isAncestorOfImpl(path)

fun RelativePath.isAncestorOf(path: RelativePath): Boolean = isAncestorOfImpl(path)

private fun NormalizedPath.isDescendantOfImpl(path: NormalizedPath): Boolean =
    value.startsWith(path.value + NormalizedPath.PATH_SEPARATOR)

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
