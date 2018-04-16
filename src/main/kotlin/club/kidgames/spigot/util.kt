package club.kidgames.spigot

import org.yaml.snakeyaml.Yaml
import java.io.File

inline fun <reified T : Any> T?.check(message: String = "The value provided for the ${T::class.java} was invalid"): T {
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
  val metadataText = (pluginMetadataFileName.readResource("/$pluginMetadataFileName")
      ?: File(pluginMetadataFileName).takeIf { it.exists() }?.readText()
      ?: "plugins: ")

  return@lazy Yaml().load(metadataText) as StringMap
}

val pluginMetadataKey = "plugins"

val pluginFiles: Map<String, File> by lazy {
  pluginMetadata[pluginMetadataKey]
      ?.run { this as Map<String, String> }
      ?.mapValues { e -> File(e.value) }
      ?: emptyMap()
}

fun <T : Any> T.readResource(name: String): String? {
  return this::class.java.getResourceAsStream(name)
      ?.run { this.reader().readText() }
}

fun String?.writeTo(path: String) {
  this?.run {
    File(path).writeText(this)
  }
}

val serverLocation: File? by lazy {
  pluginMetadata["server"]
      ?.run { this as Map<String, String> }
      ?.get("location")
      ?.run { File(this) }
}

