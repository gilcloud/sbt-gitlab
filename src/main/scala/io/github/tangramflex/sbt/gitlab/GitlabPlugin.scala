package io.github.tangramflex.sbt.gitlab

import org.apache.ivy.util.url.{URLHandler, URLHandlerDispatcher, URLHandlerRegistry}
import sbt.Keys.*
import sbt.*

import scala.util.Try
object GitlabPlugin extends AutoPlugin {

  
  lazy val headerAuthHandler =
    taskKey[Unit]("perform auth using header credentials")

  // This plugin will load automatically
  override def trigger: PluginTrigger = allRequirements

  object autoImport {

    val gitlabProjectId = settingKey[Option[Int]](
      "Numeric ID for the gitlab project, available on the project's home page"
    )
    val gitlabGroupId = settingKey[Option[Int]](
      "Numeric ID for the gitlab group, available on the group's home page"
    )
    val gitlabCredentials = settingKey[Option[GitlabCredentials]]("")
    val gitlabDomain =
      settingKey[String]("Domain for gitlab override if privately hosted repo")
  }
  import autoImport._

  def gitlabUrlHandlerDispatcher(gitlabDomain: String, creds: GitlabCredentials): URLHandlerDispatcher =
    new URLHandlerDispatcher {
      Seq("http", "https") foreach {
        super.setDownloader(_, GitlabUrlHandler(gitlabDomain,creds))
      }
      override def setDownloader(
          protocol: String,
          downloader: URLHandler
      ): Unit = {}
    }

  override def projectSettings: Seq[Def.Setting[_]] =
    inScope(publish.scopedKey.scope)(gitLabProjectSettings)


  val gitLabProjectSettings : Seq[Def.Setting[_]] = 
    Seq(
      publishMavenStyle := true,
      gitlabDomain := sys.env.getOrElse("CI_SERVER_HOST", "gitlab.com"),
      gitlabProjectId := sys.env
        .get("CI_PROJECT_ID")
        .flatMap(str => Try(str.toInt).toOption),
      gitlabGroupId := sys.env
        .get("CI_GROUP_ID")
        .flatMap(str => Try(str.toInt).toOption),
      gitlabCredentials := {
        sys.env
          .get("CI_JOB_TOKEN")
          .map(GitlabCredentials("Job-Token", _))
      },
      headerAuthHandler := {
        val cred = gitlabCredentials.value.orElse {
          Credentials
            .allDirect(credentials.value.filter {
              case f: FileCredentials => f.path.exists()
              case _                  => true
            })
            .find(_.realm == "gitlab")
            .map{GitlabCredentials(_)}
        }

        val dispatcher = gitlabUrlHandlerDispatcher(gitlabDomain.value,cred.get)
        URLHandlerRegistry.setDefault(dispatcher)
      },
      update := update.dependsOn(headerAuthHandler).value,
      updateClassifiers := updateClassifiers.dependsOn(headerAuthHandler).value,
      updateSbtClassifiers := updateSbtClassifiers
        .dependsOn(headerAuthHandler)
        .value,
      publish := publish.dependsOn(headerAuthHandler).value,
      publishTo := (ThisProject / publishTo).value.orElse {
        gitlabProjectId.value.map(p => "sbt-gitlab-maven" at s"https://${gitlabDomain.value}/api/v4/projects/$p/packages/maven") orElse
        gitlabGroupId.value.map(g => "sbt-gitlab-maven" at s"https://${gitlabDomain.value}/api/v4/groups/$g/-/packages/maven")
      }
    )
}
