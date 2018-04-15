package club.kidgames.spigot

open class SpigotExtension {
  var version = "1.12.2-R0.1-SNAPSHOT"
  var serverJarLocation:String? = null
  var buildToolsVersion = "latest"
  var buildToolsLocation:String = "https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar"
  var spigotBuildDir = "spigotBuild"
  var isAcceptEula = false
  lateinit var main:String
}

