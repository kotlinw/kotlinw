package kotlinw.util

interface HasDisplayName {
    val displayName: String
}

fun interface DisplayNameProvider<T> {
    fun displayNameOf(obj: T): String
}

class DefaultDisplayNameProvider(
    private val nullLabel: String = "-"
) : DisplayNameProvider<Any?> {
    companion object {
        val instance = DefaultDisplayNameProvider()

        fun <T> instance() = instance as DisplayNameProvider<T>
    }

    override fun displayNameOf(obj: Any?) =
        when (obj) {
            null -> nullLabel
            is HasDisplayName -> obj.displayName
            else -> obj.toString()
        }
}
