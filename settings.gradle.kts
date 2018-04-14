import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.repositories

rootProject.name = "SpigotTestServer"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        maven("https://dl.bintray.com/mverse-io/mverse-public")
    }
}

