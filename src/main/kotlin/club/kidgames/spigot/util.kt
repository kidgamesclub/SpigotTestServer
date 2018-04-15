package club.kidgames.spigot

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSetOutput
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.the
import org.yaml.snakeyaml.Yaml
import java.io.File

inline fun <reified T> T.check(message: String = "The value provided for the ${T::class.java} was invalid"): T {
  return this ?: throw IllegalArgumentException(message)
}

fun <T> T.aside(fn: T.() -> Unit): T {
  fn()
  return this
}

fun String?.asResource(clazz: Class<*> = SpigotExtension::class.java): String? {
  return this
      ?.let(clazz::getResourceAsStream)
      ?.run { this.reader().readText() }
}

val pluginMetadataFileName = "plugin-metadata.yml"
typealias StringMap = Map<String, Any?>

val pluginMetadata: StringMap by lazy {
  pluginMetadataFileName.asResource()?.run {
    return@lazy try {
      Yaml().load(this) as StringMap
    } catch (e: Exception) {
      null as StringMap
    }
  } ?: emptyMap<String, Any?>()
}

val pluginMetadataKey = "plugins"

val pluginFiles: Map<String, File> by lazy {
  pluginMetadata[pluginMetadataKey]
      ?.run { this as Map<String, String> }
      ?.mapValues { e -> File(e.value) }
      ?: emptyMap()
}

val Project.mainOutput: SourceSetOutput
  get() {
    return the<JavaPluginConvention>()
        .sourceSets["main"]
        .output
  }

val serverLocation: File? by lazy {
  pluginMetadata["server"]
      ?.run { this as Map<String, String> }
      ?.get("location")
      ?.run { File(this) }
}
