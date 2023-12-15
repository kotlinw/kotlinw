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

## Libraries

- [`kotlinw-eventbus-inprocess`](doc/kotlinw-eventbus-inprocess.md): Provides an in-process event bus mechanism based on Kotlin coroutines.
