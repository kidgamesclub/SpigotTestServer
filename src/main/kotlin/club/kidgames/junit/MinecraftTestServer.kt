package club.kidgames.junit

import net.minecraft.server.v1_12_R1.ExceptionWorldConflict
import net.minecraft.server.v1_12_R1.MinecraftServer
import org.bukkit.Bukkit
import org.bukkit.Server
import org.bukkit.craftbukkit.libs.joptsimple.OptionParser
import org.bukkit.craftbukkit.v1_12_R1.CraftServer
import org.junit.rules.ExternalResource
import java.io.File
import java.util.concurrent.atomic.AtomicReference

class MinecraftTestServer : ExternalResource() {

  private val server = AtomicReference<Server>()

  val craftServer: Server
    get() = server.get()!!

  val minecraft: MinecraftServer
    get() {
      val c = craftServer
      return when (c) {
        is CraftServer -> c.server!!
        else -> throw NullPointerException("No server available")
      }
    }

  override fun before() {
    val parser = OptionParser(true)
    parser
        .accepts("config")
        .withRequiredArg()
        .ofType(File::class.java)
        .defaultsTo(File("config.yml"))

    parser
        .accepts("bukkit-settings")
        .withRequiredArg()
        .ofType(File::class.java)
        .defaultsTo(File("bukkit-settings.yml"))

    parser
        .accepts("commands-settings")
        .withRequiredArg()
        .ofType(File::class.java)
        .defaultsTo(File("commands-settings.yml"))

    parser
        .accepts("plugins")
        .withRequiredArg()
        .ofType(File::class.java)
        .defaultsTo(File("plugins"))

    parser
        .accepts("spigot-settings")
        .withRequiredArg()
        .ofType(File::class.java)
        .defaultsTo(File("spigot-settings.yml"))

    val options = parser.parse()
    MinecraftServer.main(options)

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

  fun <P> loadPlugin(type: Class<P>): P {
    val pluginLocation = this::class.java.protectionDomain.codeSource.location
    return craftServer.pluginManager.loadPlugin(File(pluginLocation.toURI())) as P
  }

  override fun after() {
    if (server.get() != null) {
      try {
        minecraft.stop()
      } catch (exceptionWorldConflict: ExceptionWorldConflict) {
        throw RuntimeException(exceptionWorldConflict)
      }
    }
  }
}
