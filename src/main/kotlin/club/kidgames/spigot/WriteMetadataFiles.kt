package club.kidgames.spigot

import net.minecraft.server.v1_12_R1.SoundEffects.it
import org.gradle.api.internal.AbstractTask
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getValue

open class WriteMetadataFiles : AbstractTask() {

  @TaskAction
  fun writeMetadataFiles() {
    val buildDir = project.buildDir
    val mainOutput = project.javaSourceSets["main"].output
    val pluginMetadataFile = project.testServerDir
        .apply { mkdirs() }
        .resolve("plugin-metadata.yml")

    val spigotPlugin by project.configurations

    val writer = pluginMetadataFile.writer()
    val jarName = "${project.name}-${project.version}.jar"
    val jarPath = buildDir.resolve("libs").resolve(jarName)
    writer.write("plugins:\n")
    writer.write("  ${project.name}: ${jarPath.absolutePath}\n")
    spigotPlugin.resolvedConfiguration.firstLevelModuleDependencies.forEach {plugin->
      plugin.moduleArtifacts.firstOrNull()
          ?.file?.absolutePath?.run {
            writer.write("  ${plugin.moduleName}: $path\n")
          }
    }
    buildDir.resolve("lib").walkTopDown().forEach { file ->
      val parts = file.name.split(":")
      if (parts.isNotEmpty() && parts[0].isNotEmpty()) {
        writer.write("  ${parts[0]}: ${file.name}")
      }
    }

    writer.write("server:\n")
    writer.write("  location: ${project.testServerDir}\n")
    writer.flush()
    writer.close()
  }
}
