import org.gradle.internal.impldep.org.bouncycastle.asn1.x500.style.RFC4519Style.name
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.repositories

rootProject.name = "club.kidgames.spigot.gradle.plugin"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        maven("https://dl.bintray.com/mverse-io/mverse-public")
    }
}

