name := "sbt-gitlab"
organization := "com.gilcloud"
version := "0.0.5"
description := "publishing and dependency resolution for gitlab both private and hosted using header auth"
sbtPlugin := true

licenses += ("Apache-2.0", url("http://opensource.org/licenses/Apache-2.0"))
developers := List(Developer("gilandose", "Richard Gilmore", "richard.gilmore  gmail com", url("http://gilcloud.com/")))
startYear := Some(2020)
homepage := scmInfo.value map (_.browseUrl)
scmInfo := Some(ScmInfo(url("https://github.com/gilcloud/sbt-gitlab"), "scm:git:git@github.com:gilcloud/sbt-gitlab.git"))


bintrayPackageLabels := Seq("sbt", "plugin")
bintrayVcsUrl := Some("https://github.com/gilcloud/sbt-gitlab.git")
publishMavenStyle := false
bintrayRepository := "sbt-plugins"
bintrayOrganization := Some("gilcloud")

initialCommands in console := "import com.gilcloud.sbt.gitlab._"

enablePlugins(ScriptedPlugin)
// set up 'scripted; sbt plugin for testing sbt plugins
scriptedLaunchOpts ++=
  Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
