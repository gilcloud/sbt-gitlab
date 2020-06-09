name := "sbt-gitlab"
organization := "net.thecoda"
version := "0.0.3"

sbtPlugin := true

licenses += ("Apache-2.0", url("http://opensource.org/licenses/Apache-2.0"))
// ScalaTest
//libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.1" % "test"
//libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

bintrayPackageLabels := Seq("sbt", "plugin")
bintrayVcsUrl := Some("git@github.com:thecoda/sbt-gitlab.git")
bintrayRepository := "maven"

initialCommands in console := "import net.thecoda.sbt.gitlab._"

enablePlugins(ScriptedPlugin)
// set up 'scripted; sbt plugin for testing sbt plugins
scriptedLaunchOpts ++=
  Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
