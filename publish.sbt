ThisBuild / scalaVersion := "2.12.17"
ThisBuild / version := "0.1.2"
ThisBuild / organization := "com.gilcloud"
ThisBuild / organizationName := "gilcloud"
ThisBuild / organizationHomepage := Some(url("http://gilcloud.com/"))
sonatypeCredentialHost := "s01.oss.sonatype.org"
sonatypeRepository := "https://s01.oss.sonatype.org/service/local"
sonatypeProfileName := "com.gilcloud"

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/gilcloud/sbt-gitlab"),
    "scm:git:git@github.com:gilcloud/sbt-gitlab.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id = "gilandose",
    name = "Richard Gilmore",
    email = "richard.gilmore@gmail.com",
    url = url("http://gilcloud.com")
  )
)

ThisBuild / description := "gitlab to sbt integrations"
ThisBuild / licenses := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / homepage := Some(url("https://github.com/gilcloud/sbt-gitlab"))

// Remove all additional repository other than Maven Central from POM
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishTo := sonatypePublishToBundle.value
ThisBuild / publishMavenStyle := true
