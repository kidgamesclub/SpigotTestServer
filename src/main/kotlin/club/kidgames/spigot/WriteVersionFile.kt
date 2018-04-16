package club.kidgames.spigot

import org.gradle.api.internal.AbstractTask
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getValue
import org.yaml.snakeyaml.Yaml

open class WriteVersionFile : AbstractTask() {

  @TaskAction
  fun writeVersionFile() {
    project.buildDir
        .apply {mkdirs()}
        .resolve("mversion")
        .writeText(project.version.toString())
  }
}
