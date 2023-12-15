# kotlinw

## Usage

The library is published to the Maven Central, simply add the appropriate module as a dependency in your `build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("xyz.kotlinw:$MODULE_ID:$LATEST_VERSION")
            }
        }
    }
}
```

## Modules

- [In-process event bus](doc/kotlinw-eventbus-inprocess.md): an in-process event bus mechanism based on Kotlin coroutines and flows
- [Remoting](doc/kotlinw-remoting.md): a flexible remoting implementation based on _ktor_ and _kotlinx-serialization_
