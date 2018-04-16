package club.kidgames.spigot

import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.repositories
import java.net.URL

open class BukkitBuildPlugin : Plugin<Project> {
  lateinit var project: Project
  /**
   * Apply this plugin to the given target object.
   *
   * @param project The target object
   */
  override fun apply(target: Project) {
    // Add a source set
    this.project = target
    project.extensions.add("spigot", SpigotExtension())

    project.run {
      repositories {
        mavenLocal()
      }

      afterEvaluate {

        val spigot: SpigotExtension by project.extensions

        dependencies {
          "compile"("org.spigotmc:spigot-api:${spigot.version}")
          "compile"(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
          if (spigot.serverJarLocation != null) {
            "compile"(files(spigot.serverJarLocation!!))
          }
        }

        val buildToolsURI = when (spigot.buildToolsLocation.toLowerCase().startsWith("http")) {
          true -> URL(spigot.buildToolsLocation)
          else -> file(spigot.buildToolsLocation).toURI().toURL()
        }

        val spigotBuildDir = file(spigot.spigotBuildDir).absoluteFile
        if (!spigotBuildDir.exists()) {
          spigotBuildDir.mkdirs()
        }

        val buildToolsJarFile = spigotBuildDir.resolve("BuildTools.jar")

        val pluginsFolder = testServerDir.resolve("plugins")
        val existingServerJar = project.getSpigotServer()

        val copyProjectPlugin by tasks.creating(Copy::class.java) {
          dependsOn("jar")
          from(buildDir.resolve("libs"))
          into(pluginsFolder)
        }

        val downloadBuildTool by tasks.creating(Download::class.java) {
          src(buildToolsURI)
          dest(buildToolsJarFile)
        }

        val buildSpigot by tasks.creating(JavaExec::class.java) {
          //Attempt to resolve spigot
          if (existingServerJar != null) {
            isEnabled = false
            dependsOn()
          } else {
            if (!buildToolsJarFile.exists()) {
              dependsOn(downloadBuildTool)
            }
          }
          main = "-jar"
          args(buildToolsJarFile.absolutePath, "--rev", spigot.buildToolsVersion)
          workingDir = spigotBuildDir
        }
      }
    }
  }
}
