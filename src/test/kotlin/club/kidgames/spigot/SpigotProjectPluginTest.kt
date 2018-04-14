package club.kidgames.spigot

import groovy.text.SimpleTemplateEngine
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GFileUtils.copyDirectory
import org.gradle.util.GFileUtils.copyFile
import org.gradle.util.GFileUtils.writeFile
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets.UTF_8

val debugPort = Integer.parseInt(System.getenv("debugPort") ?: "0")
val tmpOutput = File("build/gradle-tests/")
val version = File("build/mversion").readText()

class SpigotProjectPluginTest {
  init {
    tmpOutput.mkdirs()
  }

  private var buildFile: File? = null
  private var testProjectDir: File = File("")
  private var gradleRunner: GradleRunner? = null
  private var projectName: String? = null

  val isDebug: Boolean
    get() = debugPort > 0

  @Test
  fun testSimpleGradleProject() {
    val result = setUpTestProject("spigot")
        .withArguments("clean", "build", "--stacktrace")
        .build()
  }

//  @Test
//  fun testSimpleGradleProjectTasks() {
//    val result = setUpTestProject("spigot")
//        .withArguments("tasks", "--stacktrace")
//        .build()
//
//    assertThat(result).isNotNull()
//  }

  private fun testProjectPath(path: String): File {
    return tmpOutput.resolve(path)
  }

  /**
   * Helper method for copying files
   * @param paths
   */
  private fun copyToTestProject(vararg paths: String) {
    paths.forEach { name ->
      val file = File(name)
      if (file.isDirectory) {
        println("\t- Copying local directory $name from project root to testDir")
        copyDirectory(file, testProjectPath(name))
      } else {
        println("\t- Copying local file $name from project root to testDir")
        copyFile(file, testProjectPath(name))
      }
    }
  }

  fun setUpTestProject(folder: String): GradleRunner {
    projectName = folder

    testProjectDir = tmpOutput.resolve(folder)
    testProjectDir.mkdirs()

    println("\t- Copying project $folder from testProjects/$folder to ${testProjectDir.name}")
    copyDirectory(File("testProjects/$folder"), testProjectDir)

    val props = mutableMapOf<String, Any>(
        "projectVersion" to version
    )

    copyToTestProject("lib", "gradlew", "gradlew.bat", "gradle", ".gradle")
    buildFile = File(testProjectDir, "build.gradle")
    val templateEngine = SimpleTemplateEngine(false)

    testProjectDir.walkTopDown().forEach { visited ->
      if (visited.name.endsWith(".template")) {
        val actualFileName = visited.name.replace(".template", "")
        println("\t- Processing template for $actualFileName")
        val outputFile = File(visited.parentFile, actualFileName)
        if (outputFile.exists()) {
          outputFile.delete()
        }
        val fileTemplate = templateEngine.createTemplate(visited)
        fileTemplate.make(props).writeTo(outputFile.bufferedWriter(UTF_8))
        visited.delete()
      }
    }

    if (!testProjectPath("settings.gradle").exists()) {
      println("\t- Writing settings.gradle with project name")
      writeFile("rootProject.name = '$folder'", testProjectPath("settings.gradle"))
    }

    println("Copying generated testkit gradle properties (supports codecov and other stuff)")

    //todo: Load from classpath
    var testkitProperties = File("build/testkit/testkit-gradle.properties")
        .readText(Charsets.UTF_8)
    if (this.isDebug) {
      checkDebugPort()
      testkitProperties += " -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=$debugPort"
    }

    testkitProperties += " -Xmx2g"
    writeFile(testkitProperties,
        File(testProjectDir, "gradle.properties"))

    Assertions.assertThat(buildFile).exists()
    println("Starting tests...")
    gradleRunner = GradleRunner.create()
        .withProjectDir(testProjectDir)
        .forwardOutput()
        .withPluginClasspath()
    return gradleRunner!!
  }

  fun checkDebugPort(): Boolean {
    try {
      //      Socket socket = new Socket()
      //      socket.setKeepAlive(true)
      //      socket.setSoLinger(true, Integer.MAX_VALUE)
      //      socket.connect(new InetSocketAddress(debugPort), 100)
      println("Connect debugger on port $debugPort")
      return true
    } catch (e: Exception) {
      println("Expected debugging server on port $debugPort but couldn't connect: $e")
      return false
    }
  }
}
