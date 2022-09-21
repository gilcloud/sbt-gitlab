package io.github.tangramflex.sbt.gitlab

import lmcoursier.CoursierConfiguration
import lmcoursier.definitions.Authentication
import org.apache.ivy.util.url.{URLHandler, URLHandlerDispatcher, URLHandlerRegistry}
import sbt.Keys.*
import sbt.*

import scala.util.Try
object GitlabPlugin extends AutoPlugin {

  
  lazy val headerAuthHandler =
    taskKey[Unit]("perform auth using header credentials")

  // Not sure we have any reason to expose this
  private val gitlabResolvers = settingKey[Seq[MavenRepository]]("List of gitlab Maven repositories")
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

  override def projectSettings: Seq[Def.Setting[_]] = {
    inScope(publish.scopedKey.scope)(gitLabProjectSettings)
  }

  private val gitlabCredentialsHandler = Def.task {
    gitlabCredentials.value.orElse {
      Credentials
        .allDirect(credentials.value.filter {
          case f: FileCredentials => f.path.exists()
          case _ => true
        })
        .find(_.realm == "gitlab")
        .map {
          GitlabCredentials(_)
        }
    }.get
  }

  private def projectRepo(domain: String, projectId: Int): MavenRepository =
    s"gitlab-maven-project-$projectId" at s"https://$domain/api/v4/projects/$projectId/packages/maven"

  private def groupRepo(domain: String, groupId: Int): MavenRepository =
    s"gitlab-maven-group-$groupId" at s"https://$domain/api/v4/groups/$groupId/-/packages/maven"

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
        val cred = gitlabCredentialsHandler.value
        val dispatcher = gitlabUrlHandlerDispatcher(gitlabDomain.value, cred)
        URLHandlerRegistry.setDefault(dispatcher)
      },
      update := update.dependsOn(headerAuthHandler).value,
      updateClassifiers := updateClassifiers.dependsOn(headerAuthHandler).value,
      updateSbtClassifiers := updateSbtClassifiers
        .dependsOn(headerAuthHandler)
        .value,
      // Add in resolvers for project and group id's for domain
      gitlabResolvers := Seq(
        gitlabProjectId.value.map(pid => projectRepo(gitlabDomain.value,pid)),
        gitlabGroupId.value.map(gid => groupRepo(gitlabDomain.value,gid))
      ).flatten,
      resolvers ++= gitlabResolvers.value,
      // Adds Coursier repository Authentication for each gitlabResolver
      csrConfiguration := gitlabResolvers.value.foldRight(csrConfiguration.value){
        case (repo, csr) => csr.addRepositoryAuthentication(repo.name, Authentication(Seq(gitlabCredentialsHandler.value.key -> gitlabCredentialsHandler.value.value)))
      },
      publish := publish.dependsOn(headerAuthHandler).value,
      // If no publish location is specified then publish to the project id (if set)
      // Note: The project ID should always be set automatically with a gitlab ci pipeline via the CI_PROJECT_ID
      publishTo := (ThisProject / publishTo).value.orElse ( gitlabProjectId.value.map(pid => projectRepo(gitlabDomain.value,pid)) )
    )
}
