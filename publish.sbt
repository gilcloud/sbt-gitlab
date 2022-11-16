ThisBuild / scalaVersion := "2.13.8"
ThisBuild / version := "0.1.2"
ThisBuild / organization := "com.gilcloud.fs2"
ThisBuild / organizationName := "gilcloud"
ThisBuild / organizationHomepage := Some(url("http://gilcloud.com/"))
sonatypeCredentialHost := "s01.oss.sonatype.org"
sonatypeRepository := "https://s01.oss.sonatype.org/service/local"
sonatypeProfileName := "com.gilcloud"

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/gilcloud/fs2-sink"),
    "scm:git@github.com:gilcloud/fs2-sink.git"
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

ThisBuild / description := "commons sinks for using with fs2"
ThisBuild / licenses := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / homepage := Some(url("https://github.com/gilcloud/fs2-sink"))

// Remove all additional repository other than Maven Central from POM
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishTo := sonatypePublishToBundle.value
ThisBuild / publishMavenStyle := true

