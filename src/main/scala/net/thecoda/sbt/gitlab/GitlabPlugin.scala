package net.thecoda.sbt.gitlab

import org.apache.ivy.util.url._
import sbt.Keys._
import sbt.librarymanagement.ivy.Credentials
import sbt.{Def, _}

import scala.util.Try

object GitlabPlugin extends AutoPlugin {

  lazy val addGitlabAuth = taskKey[Unit]("")

  object autoImport {
    val gitlabProjectId = settingKey[Option[Int]](
      "Numeric ID for the gitlab project, available on the project's home page"
    )
  }
  import autoImport._

  // This plugin will load automatically
  override def trigger: PluginTrigger = allRequirements

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
      publishMavenStyle := true,
      addGitlabAuth := {
        val logger = streams.value.log
        val cred   = credentials.value
        val filteredCred = cred.filter {
          case f: FileCredentials => f.path.exists()
          case _                  => true
        }
        val creds = Credentials.allDirect(filteredCred)
        creds.find(_.realm == "gitlab") foreach { cred =>
          logger.info(
            "Gitlab credentails detected, using token-based authentiactionr"
          )
          URLHandlerRegistry.setDefault(
            new GitlabURLHandlerDispatcher(cred.userName, cred.passwd, logger)
          )
        }
        creds.filter(_.realm != "gitlab")
      },
      update := update.dependsOn(addGitlabAuth).value,
      updateClassifiers := updateClassifiers.dependsOn(addGitlabAuth).value,
      updateSbtClassifiers := updateSbtClassifiers
        .dependsOn(addGitlabAuth)
        .value,
      publish := publish.dependsOn(addGitlabAuth).value,
      gitlabProjectId := sys.env
        .get("CI_PROJECT_ID")
        .flatMap(str => Try(str.toInt).toOption),
      publishTo := gitlabProjectId.value map { p =>
        "gitlab-maven" at s"https://gitlab.com/api/v4/projects/$p/packages/maven"
      },
      credentials ++= sys.env.get("CI_JOB_TOKEN").map { token =>
        Credentials(
          realm = "gitlab",
          host = "",
          userName = "Job-Token",
          passwd = token
        )
      }
    )
}
