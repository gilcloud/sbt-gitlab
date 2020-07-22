name := "sbt-gitlab"
organization := "com.gilcloud"
version := "0.0.25"

sbtPlugin := true

licenses += ("Apache-2.0", url("http://opensource.org/licenses/Apache-2.0"))
// ScalaTest
//libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.1" % "test"
//libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

bintrayPackageLabels := Seq("sbt", "plugin")
bintrayVcsUrl := Some("git@github.com:gilcloud/sbt-gitlab.git")
bintrayRepository := "maven"

initialCommands in console := "import com.gilcloud.sbt.gitlab._"

enablePlugins(ScriptedPlugin)
// set up 'scripted; sbt plugin for testing sbt plugins
scriptedLaunchOpts ++=
  Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
