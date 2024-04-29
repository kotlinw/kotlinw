import DevelopmentMode.Development
import DevelopmentMode.Production
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import java.io.File
import java.util.*

fun File.getLocalPropertyValue(key: String): String? =
    resolve("local.properties").let {
        if (it.exists()) {
            Properties().apply { load(it.reader()) }.getProperty(key)
        } else {
            null
        }
    }

fun Project.getLocalPropertyValue(key: String): String? = rootProject.projectDir.getLocalPropertyValue(key)

fun Settings.getLocalPropertyValue(key: String): String? = this.rootDir.getLocalPropertyValue(key)

private const val developmentModeOptionName = "kotlinw.build.mode"
enum class DevelopmentMode {
    Development,
    Production
}

// TODO cleanup
val Project.buildMode: DevelopmentMode
    get() {
        val developmentModeSystemPropertyValue = System.getProperty(developmentModeOptionName)

        fun determineBuildMode(value: String?): DevelopmentMode =
            DevelopmentMode.values().firstOrNull { it.name.lowercase() == value?.lowercase() }
                ?: throw IllegalStateException("Invalid build mode: $value")

        return if (developmentModeSystemPropertyValue != null) {
            // TODO log
            determineBuildMode(developmentModeSystemPropertyValue)
        } else {
            determineBuildMode(getLocalPropertyValue(developmentModeOptionName))
        }
    }

data class SigningData(
    val privateKey: String,
    val password: String
)

fun Project.readSigningData(): SigningData {
    check(buildMode == Production)

    return SigningData(
        getLocalPropertyValue("signing.privateKey")!!,
        getLocalPropertyValue("signing.password")!!
    )
}

data class OssrhAccountData(
    val username: String,
    val password: String
)

fun Project.readOssrhAccountData(): OssrhAccountData {
    check(buildMode == Production)

    return OssrhAccountData(
        getLocalPropertyValue("ossrh.username")!!,
        getLocalPropertyValue("ossrh.password")!!
    )
}

fun Project.isDevelopmentBuildMode(): Boolean = buildMode == Development

fun Project.isNativeTargetEnabled(): Boolean = getLocalPropertyValue("com.erinors.targets.native.enable") != "false"
