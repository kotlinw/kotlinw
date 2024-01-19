plugins {
    `kotlinw-js-library`
    id("org.jetbrains.compose")
}

kotlin {
    sourceSets {
        val jsMain by getting {
            dependencies {
                api(projects.kotlinw.module.kotlinwModuleWebappCore)
                api(libs.kotlinjs.wrappers.browser)
                api(compose.html.core)
                api(compose.html.svg)
                api(compose.runtime)
                api(libs.kobweb.compose.core)
                api(libs.kobweb.compose.html.ext)
            }
        }
    }
}

compose {
    kotlinCompilerPlugin.set(dependencies.compiler.forKotlin("1.9.21"))
    kotlinCompilerPluginArgs.add("suppressKotlinVersionCompatibilityCheck=1.9.22")
}
