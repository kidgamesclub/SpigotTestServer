package club.kidgames.spigot

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.SourceSetOutput
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.the
import org.yaml.snakeyaml.Yaml
import java.io.File


internal val Project.javaSourceSets: SourceSetContainer
  get() {
    return the<JavaPluginConvention>().sourceSets
  }

internal val Project.testServerDir: File
  get() {
    return buildDir.resolve("testServer")
  }

internal val Project.pluginsFolder: File
  get() {
    return this.testServerDir.resolve("plugins")
  }

internal val Project.mainOutput: SourceSetOutput
  get() {
    return the<JavaPluginConvention>()
        .sourceSets["main"]
        .output
  }

internal fun Project.getSpigotServer(): File? {
  val spigot: SpigotExtension by this.extensions

  val declaredServerJarLocation = spigot.serverJarLocation
  val serverJar = if (declaredServerJarLocation != null) this.file(declaredServerJarLocation) else null
  if (serverJar != null && !serverJar.exists()) {
    throw GradleException("Specified server jar location $serverJar but ${serverJar.absoluteFile} doesn't exist")
  } else if (serverJar != null) {
    return serverJar
  } else {
    val spigotServer by this.configurations.creating

    return try {
      spigotServer.aside {
        val dependency = DefaultExternalModuleDependency("org.spigotmc", "spigot", spigot.version)
        dependency.isTransitive = false
        this.dependencies.add(dependency)
      }.resolve().firstOrNull()
    } catch (e: Exception) {
      null  //Error resolving.  Need to manually process
    } finally {
      this.configurations.remove(spigotServer)
    }
  }
}

internal val EULA = "By changing the setting below to TRUE you are indicating your agreement to our EULA (https://account.mojang.com/documents/minecraft_eula)."
