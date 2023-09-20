plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    targetHierarchy.default()

    jvm {
        jvmToolchain(19)
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 19)
        }
    }

    js(IR) {
        browser()
    }

    if (isNativeTargetEnabled()) {
        mingwX64()
        linuxX64()
    }
}
