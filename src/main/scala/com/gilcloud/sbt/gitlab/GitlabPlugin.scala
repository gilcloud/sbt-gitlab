package com.gilcloud.sbt.gitlab

import okhttp3.OkHttpClient
import org.apache.ivy.util.url.{
  URLHandler,
  URLHandlerDispatcher,
  URLHandlerRegistry
}
import sbt.Keys._
import sbt.internal.CustomHttp
import sbt.internal.librarymanagement.ivyint.GigahorseUrlHandler
import sbt.{Credentials, Def, _}

import scala.util.Try
object GitlabPlugin extends AutoPlugin {

  lazy val headerAuthHandler =
    taskKey[Unit]("perform auth using header credentials")
  // This plugin will load automatically

  object autoImport {

    val gitlabProjectId = settingKey[Option[Int]](
      "Numeric ID for the gitlab project, available on the project's home page"
    )
    val gitlabCredentials = settingKey[Option[GitlabCredentials]]("")
    val gitlabDomain =
      settingKey[String]("Domain for gitlab override if privately hosted repo")
  }
  import autoImport._

  override def trigger: PluginTrigger = allRequirements

  override def globalSettings =
    Seq(
      gitlabDomain := "gitlab.com"
    )

  def headerEnrichingClientBuilder(
      existingBuilder: OkHttpClient.Builder,
      domain: String,
      optCreds: Option[GitlabCredentials],
      optLogger: Option[Logger] = None
  ): OkHttpClient.Builder =
    optCreds match {
      case Some(credentials) =>
        optLogger.foreach(_.info("building gitlab custom http client"))
        existingBuilder
          .addNetworkInterceptor(HeaderInjector(credentials, domain, optLogger))
      case None =>
        existingBuilder
    }

  def dispatcherForClient(client: OkHttpClient): URLHandlerDispatcher =
    new URLHandlerDispatcher {
      Seq("http", "https") foreach {
        super.setDownloader(_, new GigahorseUrlHandler(client))
      }

      override def setDownloader(
          protocol: String,
          downloader: URLHandler
      ): Unit = {}
    }

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
      publishMavenStyle := true,
      gitlabProjectId := sys.env
        .get("CI_PROJECT_ID")
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
            .map { GitlabCredentials(_) }
        }
        val logger = streams.value.log
        val client = headerEnrichingClientBuilder(
          CustomHttp.okhttpClientBuilder.value,
          gitlabDomain.value,
          cred,
          Some(logger)
        ).build()
        val dispatcher = dispatcherForClient(client)
        URLHandlerRegistry.setDefault(dispatcher)
      },
      update := update.dependsOn(headerAuthHandler).value,
      updateClassifiers := updateClassifiers.dependsOn(headerAuthHandler).value,
      updateSbtClassifiers := updateSbtClassifiers
        .dependsOn(headerAuthHandler)
        .value,
      publish := publish.dependsOn(headerAuthHandler).value,
      publishTo := (ThisProject / publishTo).value.orElse {
        gitlabProjectId.value map { p =>
          "gitlab-maven" at s"https://${gitlabDomain.value}/api/v4/projects/$p/packages/maven"
        }
      }
    )
}
