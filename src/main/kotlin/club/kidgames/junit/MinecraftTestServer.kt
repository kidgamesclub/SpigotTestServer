package club.kidgames.junit

import club.kidgames.spigot.check
import club.kidgames.spigot.pluginFiles
import club.kidgames.spigot.pluginMetadata
import club.kidgames.spigot.serverLocation
import org.bukkit.Bukkit
import org.bukkit.Server
import org.bukkit.craftbukkit.Main
import org.bukkit.craftbukkit.v1_12_R1.CraftServer
import org.bukkit.plugin.PluginManager
import org.junit.rules.ExternalResource
import java.io.File
import java.util.concurrent.atomic.AtomicReference

class MinecraftTestServer : ExternalResource() {

  private val server = AtomicReference<Server>()

  val craftServer: Server
    get() = server.get()!!

  override fun before() {

    val serverDir = serverLocation ?: File(".")
    val isSameDir = serverDir == File(".")

    if (!isSameDir) {
      serverDir.resolve("eula.txt").readText().run {
        File(".").resolve("eula.txt").writeText(this)
      }
    }

    Main.main(arrayOf("--plugins ${serverDir.resolve("plugins").absolutePath}",
        "--world-dir ${serverDir.absolutePath}"))

    var count = 0

    while (server.get() == null && count++ < 20) {
      try {
        Thread.sleep(1000)
      } catch (e: InterruptedException) {
        throw RuntimeException(e)
      }

      val server = Bukkit.getServer()
      if (server != null) {
        if (server !is CraftServer) {
          throw IllegalStateException("Server not started correctly")
        } else {
          this.server.set(server)
        }
      }
    }
  }

  val pluginManager: PluginManager
    get() = craftServer.pluginManager

  fun <P> loadPlugin(name: String): P {
    return when {
      pluginManager.isPluginEnabled(name) -> pluginManager.getPlugin(name)
      else -> pluginFiles[name]
          ?.run { craftServer.pluginManager.loadPlugin(this) }
          ?.check("No plugin exists with the key $name. These are " +
              "the keys we know about: ${pluginMetadata.keys}")
    } as P
  }

  override fun after() {
    server.get()?.shutdown()
  }
}
