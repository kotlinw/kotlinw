package kotlinw.configuration.core

enum class DeploymentMode {
    Development, Production;

    companion object {

        fun of(value: String) = values().first { it.name.lowercase() == value }
    }
}
