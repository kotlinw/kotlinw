plugins {
    `kotlinw-js-library`
}

kotlin {
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(npm("oauth4webapi", "2.3.0"))
            }
        }
        val jsTest by getting
    }
}
