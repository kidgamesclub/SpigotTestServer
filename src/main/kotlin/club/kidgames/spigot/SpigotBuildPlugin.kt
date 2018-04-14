package club.kidgames.spigot

import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
import org.gradle.internal.impldep.bsh.commands.dir
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.repositories
import java.io.File
import java.net.URL

open class SpigotBuildPlugin : Plugin<Project> {
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

      val spigotPlugin by project.configurations.creating
      spigotPlugin.isTransitive = false

      val runtime by target.configurations
      val compile by target.configurations

      afterEvaluate {

        buildDir.mkdirs()
        buildDir.resolve("mversion").writeText(project.version.toString())

        val spigot: SpigotExtension by project.extensions

        dependencies {
          "compile"("org.spigotmc:spigot-api:${spigot.spigotVersion}")
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
        val testServer = buildDir.resolve("testServer")
        val pluginsFolder = testServer.resolve("plugins")
        val existingServerJar = project.getSpigotServer()

        val deploy by tasks.creating(Copy::class.java) {
          dependsOn("build")
          from(buildDir.resolve("libs"))
          into("final/")
        }

        val copyServerJar by tasks.creating(Copy::class.java) {
          doFirst { testServer.mkdirs() }
          from(project.getSpigotServer())
          rename { "server.jar" }
          into(testServer)
        }

        val copyPluginJars by tasks.creating(Copy::class.java) {
          doFirst {
            pluginsFolder.mkdirs()
          }

          from(spigotPlugin)
          into(pluginsFolder)
        }

        val copyServerFiles by tasks.creating(Copy::class.java) {
          doFirst { testServer.mkdirs() }
          from("config/serverfiles")
          into(testServer)
        }

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

        val serverJarTasks: Array<Any> = if (existingServerJar != null) {
          emptyArray()
        } else {
          arrayOf(buildSpigot)
        }

        val prepareDevServer by tasks.creating {
          dependsOn(*serverJarTasks, copyServerFiles, copyServerJar, copyPluginJars, copyProjectPlugin)
        }

        val startDevServer by tasks.creating(JavaExec::class.java) {
          dependsOn(prepareDevServer)
          classpath(compile, runtime)
          main = "org.bukkit.craftbukkit.Main"
          workingDir = testServer
          standardInput = System.`in`
        }

        tasks["test"].dependsOn(prepareDevServer)
      }

    }
  }
}

fun String?.toFile(): File? {
  return if (this != null && "" != this) {
    File(this)
  } else {
    null
  }
}

fun File?.withDefault(other: () -> File?): File? {
  return if (this?.exists() == true) {
    this
  } else {
    other()
  }
}

fun Project.getSpigotServer(): File? {
  val spigot: SpigotExtension by this.extensions

  val declaredServerJarLocation = spigot.serverJarLocation
  val serverJar = if (declaredServerJarLocation != null) this.file(declaredServerJarLocation) else null
  if (serverJar != null && !serverJar.exists()) {
    throw GradleException("Specified server jar location $serverJar but ${serverJar.absoluteFile} doesn't exist")
  } else if(serverJar != null) {
    return serverJar
  } else {
    val spigotServer by this.configurations.creating

    return try {
      spigotServer.withDependencies {
        val dependency = DefaultExternalModuleDependency("org.spigotmc", "spigot", spigot.spigotVersion)
        add(dependency.setTransitive(false))
      }.resolve().firstOrNull()
    } catch (e: Exception) {
      null  //Error resolving.  Need to manually process
    } finally {
      this.configurations.remove(spigotServer)
    }
  }
}
