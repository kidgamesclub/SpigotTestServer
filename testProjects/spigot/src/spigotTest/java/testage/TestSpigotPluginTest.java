package testage;

import club.kidgames.junit.MinecraftTestServer;
import club.kidgames.liquid.plugin.LiquidPlugin;
import org.junit.Rule;
import org.junit.Test;

public class TestSpigotPluginTest {

  @Rule
  public MinecraftTestServer testServer = new MinecraftTestServer("TestSpigotPlugin");

  @Test
  public void testMinecraftServer() {
    TestSpigotPlugin plugin = testServer.loadPlugin("TestSpigotPlugin");

    System.out.println(plugin.getName());
  }

}
