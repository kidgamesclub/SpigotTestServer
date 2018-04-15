package club.kidgames.spigot

import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.the
import java.io.File
import java.net.URL
import java.util.*

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

      // This makes sure all spigotPlugin declarations are also included in the compile classpath
      // for the model
      compile.extendsFrom(spigotPlugin)
      val java = the<JavaPluginConvention>()
      val spigotTest by java.sourceSets.creating
      spigotTest.compileClasspath += project.mainOutput.classesDirs
      spigotTest.compileClasspath += project.configurations["testCompile"]

      afterEvaluate {

        buildDir.mkdirs()
        buildDir.resolve("mversion").writeText(project.version.toString())

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
        val testServerDir = buildDir.resolve("testServer")
        val pluginsFolder = testServerDir.resolve("plugins")
        val existingServerJar = project.getSpigotServer()

        val spigotTests by tasks.creating(Test::class.java) {
          val self = this

          doFirst {
            project.getSpigotServer()!!.run {
              self.classpath += project.files(this)
              println("CLASSPATH = ${self.classpath}")
            }
          }
          debug = true
          workingDir = testServerDir
          classpath += spigotTest.output
//          classpath += project.configurations["testCompile"]
          testClassesDirs = spigotTest.output.classesDirs

        }

        val deploy by tasks.creating(Copy::class.java) {
          dependsOn("build")
          from(buildDir.resolve("libs"))
          into("final/")
        }

        val copyServerJar by tasks.creating(Copy::class.java) {
          doFirst { testServerDir.mkdirs() }
          from(project.getSpigotServer())
          rename { "server.jar" }
          into(testServerDir)
        }


        val eula = testServerDir.resolve("eula.txt")
        val createServerEULA by tasks.creating(Copy::class.java) {
          doFirst {
            if(!eula.exists()) {
              eula.createNewFile()
            }

            val props = Properties()
            props.load(eula.reader())
            props.setProperty("eula", spigot.isAcceptEula.toString())
            props.store(eula.writer(), EULA)
            props.store(project.mainOutput.resourcesDir.aside { mkdirs() }.resolve("eula.txt").writer(),
                EULA)
          }
          from(project.getSpigotServer())
          rename { "server.jar" }
          into(testServerDir)
        }

        val copyPluginJars by tasks.creating(Copy::class.java) {
          doFirst {
            pluginsFolder.mkdirs()
          }

          from(spigotPlugin)
          into(pluginsFolder)
        }

        val copyServerFiles by tasks.creating(Copy::class.java) {
          doFirst { testServerDir.mkdirs() }
          from("config/serverfiles")
          into(testServerDir)
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

        val writePluginYml by tasks.creating(WritePluginYml::class.java){}
        tasks["jar"].dependsOn(writePluginYml)

        val writeMetadataFiles by tasks.creating {
          doFirst {
            val pluginMetadataFile = mainOutput.resourcesDir
                .aside{ this.mkdirs() }
                .resolve("plugin-metadata.yml")

            val writer = pluginMetadataFile.writer()
            val jarName = "${project.name}-${project.version}.jar"
            val jarPath = buildDir.resolve("libs").resolve(jarName)
            writer.write("plugins:\n")
            writer.write("  ${project.name}: ${jarPath.absolutePath}\n")
            spigotPlugin.resolvedConfiguration.firstLevelModuleDependencies.forEach {
              val path: String? = it.moduleArtifacts.firstOrNull()?.file?.absolutePath
              if (path != null) {
                writer.write("  ${it.moduleName}: $path\n")
              }
            }
            buildDir.resolve("lib").walkTopDown().forEach { file ->
              val parts = file.name.split(":")
              if (parts.isNotEmpty() && parts[0].isNotEmpty()) {
                writer.write("  ${parts[0]}: ${file.name}")
              }
            }

            writer.write("server:\n")
            writer.write("  location: $testServerDir\n")
            writer.flush()
            writer.close()
          }
        }

        val prepareDevServer by tasks.creating {
          dependsOn(*serverJarTasks, createServerEULA, writePluginYml, writeMetadataFiles, copyServerFiles, copyServerJar, copyPluginJars, copyProjectPlugin)
        }

        val startDevServer by tasks.creating(JavaExec::class.java) {
          dependsOn(prepareDevServer)
          classpath(compile, runtime)
          main = "org.bukkit.craftbukkit.Main"
          workingDir = testServerDir
          standardInput = System.`in`
        }

        spigotTests.dependsOn(prepareDevServer, "test")
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

val EULA = "By changing the setting below to TRUE you are indicating your agreement to our EULA (https://account.mojang.com/documents/minecraft_eula)."
