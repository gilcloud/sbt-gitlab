name := "sbt-gitlab"
description := "publishing and dependency resolution for gitlab both private and hosted using header auth"
sbtPlugin := true

licenses += ("Apache-2.0", url("http://opensource.org/licenses/Apache-2.0"))
developers := List(Developer("gilandose", "Richard Gilmore", "richard.gilmore  gmail com", url("http://gilcloud.com/")))
startYear := Some(2020)



initialCommands in console := "import com.gilcloud.sbt.gitlab._"

enablePlugins(ScriptedPlugin)
// set up 'scripted; sbt plugin for testing sbt plugins
scriptedLaunchOpts ++=
  Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
