import org.gradle.api.artifacts.dsl.RepositoryHandler
import java.net.URI

fun RepositoryHandler.setDefaultRepositories() {
    mavenCentral()
    google()
    maven {
        url = URI("https://repo.spring.io/release")
    }
    maven {
        url = URI("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
    maven {
        url = URI("https://androidx.dev/storage/compose-compiler/repository/")
    }
    maven {
        url = URI("https://jitpack.io")
    }
    mavenLocal()
}
