package club.kidgames.spigot

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.repositories

open class SpigotBuildPlugin : Plugin<Project> {
  lateinit var project: Project
  /**
   * Apply this plugin to the given target object.
   *
   * @param target The target object
   */
  override fun apply(target: Project) {
    // Add a source set
    this.project = target

    project.extensions.add("spigot", SpigotExtension())

    project.run {
      repositories {
        mavenLocal()
      }

      configureSourceSets()

      afterEvaluate {
        val spigot: SpigotExtension by project.extensions
        configureTasks(spigot)
        configureDependencies(spigot)
      }
    }
  }

  private fun configureSourceSets() {
    // This contains all dependent plugins
    val spigotPlugin by project.configurations.creating
    spigotPlugin.isTransitive = false

    val compile by project.configurations
    val testCompile by project.configurations

    // This makes sure all spigotPlugin declarations are also included in the compile classpath
    compile.extendsFrom(spigotPlugin)

    // SpigotTest is like an integrationTest.  It allows us to write unit-test style tests that
    // load a server and plugins in the background.
    val spigotTest by project.javaSourceSets.creating
    spigotTest.compileClasspath += project.mainOutput.classesDirs
    spigotTest.compileClasspath += testCompile
  }

  private fun configureTasks(spigot: SpigotExtension) {

    project.run {
      val spigotTestTask = tasks.create(spigotTest, Test::class.java) {
        val spigotTestSourceSet = project.javaSourceSets[spigotTest]
        val testCompile by project.configurations
        val runtime by project.configurations

        workingDir = project.testServerDir
        testClassesDirs = project.javaSourceSets[spigotTest].output.classesDirs
        classpath = project.files(project.getSpigotServer(), spigotTestSourceSet.output, testCompile, runtime)
        debug = spigot.isDebug
        dependsOn("test", "jar")
      }

      val prepTasks = listOf<Task>(
          tasks.create(writeVersionFile, WriteVersionFile::class.java),
          tasks.create(writeEula, WriteEulaFile::class.java),
          tasks.create(writeMetadata, WriteMetadataFiles::class.java),
          tasks.create(writePluginYml, WritePluginYml::class.java) {
            tasks["jar"].dependsOn(this)
          },
          tasks.create(copyServerJar, Copy::class.java) {
            from(project.getSpigotServer())
            into(project.testServerDir)
            rename { "server.jar" }
          }
//          ,
//          tasks.create(copyProjectPlugin, Copy::class.java) {
//            dependsOn("jar")
//            from(buildDir.resolve("libs"))
//            into(pluginsFolder)
//          })
      )
      prepTasks.forEach { task -> spigotTestTask.dependsOn(task) }
    }
  }

  private fun configureDependencies(spigot: SpigotExtension) {
    project.run {
      dependencies {
        "compile"("org.spigotmc:spigot-api:${spigot.version}")
        "compile"(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
        if (spigot.serverJarLocation != null) {
          "compile"(files(spigot.serverJarLocation!!))
        }
      }
    }
  }
}

internal const val spigotTest = "spigotTest"
internal const val copyServerJar = "copyServerJar"
internal const val copyProjectPlugin = "copyProjectPlugin"
internal const val writeVersionFile = "writeVersionFile"
internal const val writeEula = "writeEula"
internal const val writeMetadata = "writeMetadata"
internal const val writePluginYml = "writePluginYml"
