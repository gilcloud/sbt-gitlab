name := "sbt-gitlab"
organization := "nl.zolotko.sbt"

description := "publishing and dependency resolution for gitlab both private and hosted using header auth"
sbtPlugin := true

licenses := Seq(
  "Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt")
)
developers := List(
  Developer(
    "gilandose",
    "Richard Gilmore",
    "richard.gilmore  gmail com",
    url("http://gilcloud.com/")
  ),
  Developer(
    "azolotko",
    "Alex Zolotko",
    "azolotko@gmail.com",
    url("https://github.com/azolotko")
  )
)
startYear := Some(2020)
homepage := scmInfo.value map (_.browseUrl)
scmInfo := Some(
  ScmInfo(
    url("https://github.com/azolotko/sbt-gitlab"),
    "scm:git:git@github.com:azolotko/sbt-gitlab.git"
  )
)

console / initialCommands := "import nl.zolotko.sbt.gitlab._"

enablePlugins(SbtPlugin)

scriptedBufferLog := false

scriptedLaunchOpts := (
  scriptedLaunchOpts.value ++
    Seq("-Xmx1g", "-Dsbt.gitlab.version=" + version.value)
)

publishTo := sonatypePublishToBundle.value

sonatypeCredentialHost := "s01.oss.sonatype.org"

sonatypeProfileName := "nl.zolotko.sbt"

versionScheme := Some("semver-spec")