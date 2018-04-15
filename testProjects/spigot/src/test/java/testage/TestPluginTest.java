package testage;

import club.kidgames.junit.MinecraftTestServer;
import club.kidgames.liquid.plugin.LiquidPlugin;
import org.junit.Rule;
import org.junit.Test;

public class TestPluginTest {

  @Rule
  public MinecraftTestServer testServer = new MinecraftTestServer();

  @Test
  public void testMinecraftServer() {
    LiquidPlugin plugin = testServer.loadPlugin("LiquidMessages");

    System.out.println(plugin.getName());
  }

}
