package kotlinw.project.gradle

import kotlinw.project.gradle.DevelopmentMode.Development
import kotlinw.project.gradle.DevelopmentMode.Production
import org.gradle.api.Project
import java.util.*

fun Project.getLocalPropertyValue(key: String): String? =
    rootProject.file("local.properties").let {
        if (it.exists()) {
            Properties().apply { load(it.reader()) }.getProperty(key)
        } else {
            null
        }
    }

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
            DevelopmentMode.values().firstOrNull { it.name.toLowerCase() == value?.toLowerCase() }
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

fun Project.isNativeTargetEnabled(): Boolean = getLocalPropertyValue("com.erinors.targets.native.enable") == "true"
