package testage;

import org.junit.Rule;
import org.junit.Test;

import club.kidgames.junit.MinecraftTestServer;

public class TestPluginTest {

  @Rule
  public MinecraftTestServer testServer = new MinecraftTestServer();

  @Test
  public void testMinecraftServer() {
    TestPlugin plugin = testServer.loadPlugin(TestPlugin.class);
  }

}
