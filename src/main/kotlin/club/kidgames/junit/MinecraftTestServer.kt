package club.kidgames.junit

import club.kidgames.spigot.check
import club.kidgames.spigot.pluginFiles
import club.kidgames.spigot.pluginMetadata
import club.kidgames.spigot.pluginMetadataFileName
import club.kidgames.spigot.readResource
import club.kidgames.spigot.serverLocation
import club.kidgames.spigot.writeTo
import org.bukkit.Bukkit
import org.bukkit.Server
import org.bukkit.craftbukkit.Main
import org.bukkit.craftbukkit.v1_12_R1.CraftServer
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.java.JavaPluginLoader
import org.gradle.internal.impldep.org.bouncycastle.crypto.tls.ConnectionEnd.server
import org.junit.rules.ExternalResource
import java.io.File
import java.util.concurrent.atomic.AtomicReference

class MinecraftTestServer(vararg plugins:String)  : ExternalResource() {

  private val plugins:List<String> = listOf(*plugins)

  private val server = AtomicReference<Server>()

  val craftServer: Server
    get() = server.get()!!

  override fun before() {

    val thisDir = File(".")
//    val serverDir = serverLocation ?: thisDir

    println("Running minecraft from ${thisDir.absolutePath}")
    readResource("/eula.txt").writeTo("eula.txt")

    File(pluginMetadataFileName).readText()

    Main.main(emptyArray())

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
    pluginManager.registerInterface(JavaPluginLoader::class.java)
    for (plugin in plugins) {
      this.loadPlugin<JavaPlugin>(plugin)
    }
  }

  val pluginManager: PluginManager
    get() = craftServer.pluginManager


  fun <P: Plugin> loadPlugin(name: String): P {
    val existingPlugin = pluginManager.getPlugin(name)

    val plugin = existingPlugin ?: pluginFiles[name]
        ?.run { craftServer.pluginManager.loadPlugin(this) }
        ?: throw IllegalArgumentException("Unable to create or load plugin")

    return plugin as P
  }


  fun <P:JavaPlugin> getPlugin(name: String): P {
    return pluginManager.getPlugin(name) as P? ?: throw IllegalArgumentException("Plugin not installed")
  }

  override fun after() {
    server.get()?.shutdown()
    server.set(null)
  }
}
