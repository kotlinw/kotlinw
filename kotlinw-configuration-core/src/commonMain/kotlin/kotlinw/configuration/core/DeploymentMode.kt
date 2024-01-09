package kotlinw.configuration.core

enum class DeploymentMode(val shortName: String) {
    Development("dev"), Production("prod");

    companion object {

        fun of(value: String) = entries.first { it.name.equals(value, true) }
    }
}
