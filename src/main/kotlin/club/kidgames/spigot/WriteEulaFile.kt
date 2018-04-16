package club.kidgames.spigot

import org.gradle.api.internal.AbstractTask
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getValue
import java.util.*

open class WriteEulaFile : AbstractTask() {

  @TaskAction
  fun writeEulaFile() {
    val spigot:SpigotExtension by project.extensions
    val eula = project.testServerDir
        .apply { mkdirs() }
        .resolve("eula.txt")
      if(!eula.exists()) {
        eula.createNewFile()
      }
      val props = Properties()
      props.load(eula.reader())
      props.setProperty("eula", spigot.isAcceptEula.toString())
      props.store(eula.writer(), EULA)
  }
}
