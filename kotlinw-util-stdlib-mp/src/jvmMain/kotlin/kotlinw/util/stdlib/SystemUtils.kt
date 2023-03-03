package kotlinw.util.stdlib

enum class OperatingSystemType {
    Unknown,
    Mac,
    Linux,
    Solaris,
    Windows
}

val operatingSystemType by lazy {
    val os = System.getProperty("os.name").lowercase()
    when {
        os.contains("win") -> OperatingSystemType.Windows
        os.contains("nix") || os.contains("nux") || os.contains("aix") -> OperatingSystemType.Linux
        os.contains("mac") -> OperatingSystemType.Mac
        os.contains("sunos") -> OperatingSystemType.Solaris
        else -> OperatingSystemType.Unknown
    }
}
