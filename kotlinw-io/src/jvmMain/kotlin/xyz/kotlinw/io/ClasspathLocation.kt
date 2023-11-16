package xyz.kotlinw.io

@JvmInline
value class ClasspathLocation(
    val path: AbsolutePath
) {

    companion object {

        fun of(absolutePath: String): ClasspathLocation = ClasspathLocation(AbsolutePath(absolutePath))
    }

    val name: String get() = path.lastSegment
}
