package club.kidgames.spigot

import org.gradle.api.internal.AbstractTask
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getValue
import org.yaml.snakeyaml.Yaml

open class WritePluginYml : AbstractTask() {

  @TaskAction
  fun writePluginYml() {

    val pluginYmlFile = project.mainOutput
        .resourcesDir
        .apply { mkdirs() }
        .resolve("plugin.yml")
    val pluginYml: MutableMap<String, Any?> = when {
      pluginYmlFile.exists() -> Yaml().load(pluginYmlFile.readText())
      else -> mutableMapOf()
    }

    val spigot:SpigotExtension by project.extensions

    pluginYml["name"] = project.name
    pluginYml["description"] = project.description ?: project.name

    pluginYml["version"] = project.version
    pluginYml["main"] = spigot.main

    pluginYmlFile.writeText(Yaml().dumpAsMap(pluginYml))
  }
}
