

inThisBuild(List(
  organization  := "io.github.tangramflex",
  homepage      := Some(url("https://github.com/TangramFlex/sbt-gitlab")),
  versionScheme := Some("early-semver"),
  licenses      := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  description   := "publishing and dependency resolution for gitlab both private and hosted using header auth",
  developers    :=
    Developer("gilandose", "Richard Gilmore", "richard.gilmore  gmail com", url("http://gilcloud.com/"))
    :: Developer("listba", "Ben List", "benjamin.list tangramflex com", url("https://github.com/listba/")) 
    :: Nil,
  sonatypeCredentialHost := "s01.oss.sonatype.org",
  sonatypeRepository     := "https://s01.oss.sonatype.org/service/local",
))


lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-gitlab",
    console / initialCommands := "import io.github.tangramflex.sbt.gitlab._",
    pluginCrossBuild / sbtVersion := {
      scalaBinaryVersion.value match {
        case "2.12" => "1.5.0" // set minimum sbt version
      }
    }
  )
  