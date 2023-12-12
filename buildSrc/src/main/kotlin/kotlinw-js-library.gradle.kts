import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode.DEVELOPMENT
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode.PRODUCTION

plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp")
}

kotlin {
    targetHierarchy.default()

    js(IR) {
        browser {
            webpackTask {
                mode = if (isDevelopmentBuildMode()) DEVELOPMENT else PRODUCTION
            }
        }
    }
}
