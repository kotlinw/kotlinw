# kotlinw

## Usage

The library is published to the Maven Central, simply add the appropriate module as a dependency in your `build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("xyz.kotlinw:kotlinw-util-stdlib-mp:0.0.4")
            }
        }
    }
}
```

## Modules

Group ID: `xyz.kotlinw`

- `kotlinw-util-stdlib-mp`: Utilities with only core dependencies (used by most applications like kotlinx-collections-immutable, etc.)
