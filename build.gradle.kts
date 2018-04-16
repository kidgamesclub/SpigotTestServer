import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import io.mverse.gradle.main
import io.mverse.gradle.sourceSets
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.codehaus.groovy.tools.shell.util.Logger.io
import org.gradle.api.internal.file.pattern.PatternMatcherFactory.compile
import org.gradle.internal.impldep.org.junit.experimental.categories.Categories.CategoryFilter.exclude
import org.gradle.internal.impldep.org.junit.experimental.categories.Categories.CategoryFilter.include
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCallArgument.DefaultArgument.arguments

plugins {
  id("org.gradle.kotlin.kotlin-dsl").version("0.16.0")
  id("com.github.johnrengelman.shadow").version("2.0.3")
  id("io.mverse.project").version("0.5.16")
  id("java-gradle-plugin")
  id("pl.droidsonroids.jacoco.testkit").version("1.0.3")
}

repositories {
  mavenLocal()
  jcenter()
  mavenCentral()
  maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots")
  maven("http://repo.bstats.org/content/repositories/releases/")
  maven("http://repo.dmulloy2.net/nexus/repository/public/")
}

gradlePlugin {
  plugins.create("kidgamesSpigot") {
    id = "club.kidgames.spigot"
    implementationClass = "club.kidgames.spigot.SpigotBuildPlugin"
  }
}

mverse {
  groupId = "club.kidgames"
  modules {
    compile("guava")
    compile("bukkit")
    compile("gradle-download-task")
  }

  coverageRequirement = 0.0
  dependencies.lombok = false
  dependencies.streamEx = false
  dependencies.logback = false
}

dependencyManagement {
  dependencies {
    dependency("de.undercouch:gradle-download-task:1.2")

    dependency("commons-io:commons-io:2.4")

    dependency("com.github.johnrengelman.shadow:com.github.johnrengelman.shadow.gradle.plugin:1.2.3")
    dependency("club.kidgames:liqp:0.7.13")
    dependency("club.kidgames:LiquidMessages:0.7.13")

    dependency("io.mverse.project:io.mverse.project.gradle.plugin:0.5.+")
    dependency("me.clip:PlaceholderAPI:2.5.+")
    dependency("org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT")
    dependency("org.bukkit:bukkit:1.12.2-R0.1-SNAPSHOT")
  }
}

dependencies {
  testCompile(gradleTestKit())
  implementation(gradleKotlinDsl())
  compile("commons-io:commons-io:2.4")
  compile(files("lib/spigot-1.12.2.jar"))
  testCompile("me.clip:PlaceholderAPI")
  compileOnly("org.spigotmc:spigot-api")
  shadow("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

afterEvaluate {
  buildDir.resolve("mversion").writeText(project.version.toString())
}

val testJar by tasks.creating(ShadowJar::class.java) {
  dependsOn("classes")
  configurations = listOf(project.configurations.shadow)
  from(project.sourceSets.main!!.output.classesDirs)
  classifier = null
}


val shadowJar = tasks.replace("jar", ShadowJar::class.java)
shadowJar.configurations = listOf(project.configurations.shadow)
shadowJar.from(project.sourceSets.main!!.output)
shadowJar.classifier = null

tasks["test"].dependsOn("jar")

afterEvaluate {
  publishing {
    (publications) {
      val nebula: MavenPublication by this.getting

      "spigotTestServer"(MavenPublication::class) {
        this.version = project.version.toString()
        this.groupId = "club.kidgames.spigot"
        this.artifactId = "club.kidgames.spigot.gradle.plugin"
        from(components["java"])
        setArtifacts(nebula.artifacts)
      }
    }
  }

  bintray {
    setPublications("spigotTestServer")
  }
}

