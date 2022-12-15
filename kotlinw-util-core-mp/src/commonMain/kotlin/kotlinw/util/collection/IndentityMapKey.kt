package kotlinw.util.collection

class IndentityMapKey<T>(val wrappedObject: T) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as IndentityMapKey<*>
        return wrappedObject === other.wrappedObject
    }

    override fun hashCode(): Int {
        return wrappedObject?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "IndentityMapKey(wrappedObject=$wrappedObject)"
    }
}
