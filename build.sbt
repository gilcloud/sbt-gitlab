name := """sbt-gitlab"""
organization := "com.gilandose"
version := "0.0.1-SNAPSHOT"

sbtPlugin := true


// ScalaTest
//libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.1" % "test"
//libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

//bintrayPackageLabels := Seq("sbt","plugin")
//bintrayVcsUrl := Some("""git@github.com:com.gilcloud/sbt-gitlab.git""")

initialCommands in console := """import com.gilcloud.sbt._"""

enablePlugins(ScriptedPlugin)
// set up 'scripted; sbt plugin for testing sbt plugins
scriptedLaunchOpts ++=
  Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
