package club.kidgames.spigot

import org.gradle.api.internal.AbstractTask
import org.gradle.api.tasks.TaskAction
import org.yaml.snakeyaml.Yaml

open class WritePluginYml : AbstractTask() {

  @TaskAction
  fun writePluginYml() {
    val pluginYmlFile = project.mainOutput
        .resourcesDir
        .aside { this.mkdirs() }
        .resolve("plugin.yml")
    val pluginYml: MutableMap<String, Any?> = when {
      pluginYmlFile.exists() -> Yaml().load(pluginYmlFile.readText())
      else -> mutableMapOf()
    }

    pluginYml["name"] = project.name
    pluginYml["version"] = project.version

    pluginYmlFile.writeText(Yaml().dumpAsMap(pluginYml))
  }
}
