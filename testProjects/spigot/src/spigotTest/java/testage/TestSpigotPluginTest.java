package testage;

import club.kidgames.junit.MinecraftTestServer;
import org.junit.ClassRule;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class TestSpigotPluginTest {

  @ClassRule
  public static MinecraftTestServer testServer = new MinecraftTestServer("TestSpigotPlugin");

  @Test
  public void testMinecraftServer() {
    TestSpigotPlugin plugin = testServer.getPlugin("TestSpigotPlugin");

    assertThat(plugin).isNotNull();
  }

  @Test
  public void testPluginName() {
    TestSpigotPlugin plugin = testServer.getPlugin("TestSpigotPlugin");

    assertThat(plugin.getName()).isEqualTo("TestSpigotPlugin");
  }

}
