package kotlinw.util.stdlib

import java.io.File

enum class OperatingSystemType {
    Unknown,
    Mac,
    Linux,
    Solaris,
    Windows
}

val operatingSystemType by lazy {
    val os = System.getProperty("os.name")?.lowercase()
    if (os != null) {
        when {
            os.contains("win") -> OperatingSystemType.Windows
            os.contains("nix") || os.contains("nux") || os.contains("aix") -> OperatingSystemType.Linux
            os.contains("mac") -> OperatingSystemType.Mac
            os.contains("sunos") -> OperatingSystemType.Solaris
            else -> OperatingSystemType.Unknown
        }
    } else {
        when (File.separatorChar) {
            '\\' -> OperatingSystemType.Windows
            else -> OperatingSystemType.Unknown
        }
    }
}

enum class OperatingSystemArchitecture {
    X64, AARCH64
}

val operatingSystemArchitecture by lazy {
    when (val osArchitecture = System.getProperty("os.arch")) {
        "x64", "x86_64", "amd64" -> OperatingSystemArchitecture.X64
        "aarch64", "arm64" -> OperatingSystemArchitecture.AARCH64
        else -> throw IllegalStateException("Unsupported OS architecture: $osArchitecture")
    }
}
