package net.thecoda.sbt.gitlab

import okhttp3.OkHttpClient
import org.apache.ivy.util.url.{
  URLHandler,
  URLHandlerDispatcher,
  URLHandlerRegistry
}
import sbt.Keys._
import sbt.internal.CustomHttp
import sbt.internal.librarymanagement.IvySbt
import sbt.internal.librarymanagement.ivyint.GigahorseUrlHandler
import sbt.librarymanagement.ivy.IvyPublisher
import sbt.{Credentials, Def, _}

import scala.util.Try

object GitlabPlugin extends AutoPlugin {

  val patchHandler = taskKey[Unit]("")

  object autoImport {
    case class GitlabCredentials(key: String, value: String)

    val gitlabProjectId = settingKey[Option[Int]](
      "Numeric ID for the gitlab project, available on the project's home page"
    )
    val gitlabApiToken    = settingKey[Option[String]]("")
    val gitlabCredentials = settingKey[Option[GitlabCredentials]]("")
  }
  import autoImport._

  // This plugin will load automatically
  override def trigger: PluginTrigger = allRequirements

  def headerEnrichingClientBuilder(
      existingBuilder: OkHttpClient.Builder,
      optCreds: Option[GitlabCredentials],
      optLogger: Option[Logger] = None
  ): OkHttpClient.Builder =
    optCreds match {
      case Some(credentials) =>
        optLogger.foreach(_.info("building gitlab custom http client"))
        existingBuilder
          .addNetworkInterceptor(HeaderInjector(credentials, optLogger))
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
      gitlabApiToken := sys.env.get("GITLAB_API_TOKEN"),
      gitlabCredentials := {
        sys.env
          .get("CI_JOB_TOKEN")
          .map(GitlabCredentials("Job-Token", _))
          .orElse(
            gitlabApiToken.value.map(GitlabCredentials("Private-Token", _))
          )
      },
//      CustomHttp.okhttpClientBuilder := {
//        headerEnrichingClientBuilder(
//          CustomHttp.okhttpClientBuilder.value,
//          gitlabCredentials.value
//        )
//      },
//      CustomHttp.okhttpClient :=
//        CustomHttp.okhttpClientBuilder.value.build(),
//      ivySbt := {
//        val log = streams.value.log
//        Credentials.register(credentials.value, log)
//        new IvySbt(ivyConfiguration.value, CustomHttp.okhttpClient.value)
//      },
//      publisher := IvyPublisher(
//        ivyConfiguration.value,
//        CustomHttp.okhttpClient.value
//      ),
      patchHandler := {
        val logger = streams.value.log
        val client = headerEnrichingClientBuilder(
          CustomHttp.okhttpClientBuilder.value,
          gitlabCredentials.value,
          Some(logger)
        ).build()
        val dispatcher = dispatcherForClient(client)
        URLHandlerRegistry.setDefault(dispatcher)
      },
      update := update.dependsOn(patchHandler).value,
      updateClassifiers := updateClassifiers.dependsOn(patchHandler).value,
      updateSbtClassifiers := updateSbtClassifiers
        .dependsOn(patchHandler)
        .value,
      publish := publish.dependsOn(patchHandler).value,
      publishTo := gitlabProjectId.value map { p =>
        "gitlab-maven" at s"https://gitlab.com/api/v4/projects/$p/packages/maven"
      }
    )
}
