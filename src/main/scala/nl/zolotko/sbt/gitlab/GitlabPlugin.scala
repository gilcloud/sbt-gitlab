package nl.zolotko.sbt.gitlab

import lmcoursier.CoursierConfiguration
import lmcoursier.definitions.Authentication
import okhttp3.OkHttpClient
import org.apache.ivy.util.url.{URLHandler, URLHandlerDispatcher, URLHandlerRegistry}
import sbt.Keys._
import sbt.internal.CustomHttp
import sbt.internal.librarymanagement.ivyint.GigahorseUrlHandler
import sbt.{Credentials, Def, settingKey, _}
object GitlabPlugin extends AutoPlugin {

  // This plugin will load automatically
  override def trigger: PluginTrigger = allRequirements

  override def projectSettings: Seq[Def.Setting[_]] =
    inScope(publish.scopedKey.scope)(gitlabProjectSettings)

  object autoImport {
    case class GitlabProjectId(id: String)
    case class GitlabGroupId(id: String)

    sealed trait GitlabRepository {
      def gitlabDomain: String
      def resolver: Resolver
      def resolverName: String

      protected lazy val repositoryNamePrefix: String = s"gitlab-maven-${gitlabDomain.hashCode.abs}"
    }

    final case class GitlabProjectRepository(
        gitlabDomain: String,
        projectId: GitlabProjectId
    ) extends GitlabRepository {
      override lazy val resolver =
        resolverName at s"https://$gitlabDomain/api/v4/projects/${projectId.id}/packages/maven"
      override lazy val resolverName = s"$repositoryNamePrefix-project-${projectId.id}"
    }

    final case class GitlabGroupRepository(
        gitlabDomain: String,
        groupId: GitlabGroupId
    ) extends GitlabRepository {
      override lazy val resolver = resolverName at s"https://$gitlabDomain/api/v4/groups/${groupId.id}/-/packages/maven"
      override lazy val resolverName = s"$repositoryNamePrefix-group-${groupId.id}"
    }

    val gitlabProjectId = settingKey[Option[GitlabProjectId]](
      "GitLab project ID."
    )

    val gitlabCredentials = settingKey[Option[GitlabCredentials]]("GitLab credentials.")

    val gitlabDomain = settingKey[String]("Domain for GitLab override if privately hosted repo.")

    val gitlabRepositories =
      settingKey[Seq[GitlabRepository]]("GitLab repositories for automatically managed dependencies.")
  }
  import autoImport._

  private def headerEnrichingClientBuilder(
      existingBuilder: OkHttpClient.Builder,
      domain: String,
      credentials: Seq[GitlabCredentials],
      logger: Option[Logger] = None
  ): OkHttpClient.Builder =
    credentials.find(_.domain == domain) match {
      case Some(credentials) =>
        logger.foreach(_.debug("building a custom HTTP client for GitLab"))
        existingBuilder
          .addNetworkInterceptor(HeaderInjector(credentials, domain, logger))
      case None =>
        existingBuilder
    }

  private def dispatcherForClient(client: OkHttpClient): URLHandlerDispatcher =
    new URLHandlerDispatcher {
      Seq("http", "https").foreach(super.setDownloader(_, new GigahorseUrlHandler(client)))

      override def setDownloader(protocol: String, downloader: URLHandler): Unit = ()
    }

  private lazy val gitlabHeaderAuthHandler =
    taskKey[Unit]("perform auth using header credentials")

  private val allGitlabCredentials = Def.task(
    gitlabCredentials.value.toSeq ++
      Credentials
        .allDirect(credentials.value.filter {
          case f: FileCredentials => f.path.exists()
          case _                  => true
        })
        .filter(_.realm == "gitlab")
        .map(GitlabCredentials.apply)
  )

  private def addRepositoriesAuthentication(
      coursierConfiguration: CoursierConfiguration,
      gitlabRepositories: Seq[GitlabRepository],
      gitlabCredentials: Seq[GitlabCredentials]
  ): CoursierConfiguration =
    gitlabRepositories.foldLeft(coursierConfiguration) { case (cfg, repo) =>
      gitlabCredentials
        .find(_.domain == repo.gitlabDomain)
        .fold(cfg)(token =>
          cfg.addRepositoryAuthentication(repo.resolverName, Authentication(Seq(token.key -> token.value)))
        )
    }

  private lazy val gitlabProjectSettings: Seq[Def.Setting[_]] =
    Seq(
      publishMavenStyle := true,
      gitlabDomain      := sys.env.getOrElse("CI_SERVER_HOST", "gitlab.com"),
      gitlabProjectId   := sys.env.get("CI_PROJECT_ID").map(GitlabProjectId.apply),
      gitlabCredentials := sys.env
        .get("CI_JOB_TOKEN")
        .map(GitlabCredentials(gitlabDomain.value, "Job-Token", _)),
      gitlabHeaderAuthHandler := {
        val credentials = allGitlabCredentials.value
        val logger      = streams.value.log
        val client = headerEnrichingClientBuilder(
          CustomHttp.okhttpClientBuilder.value,
          gitlabDomain.value,
          credentials,
          Some(logger)
        ).build()
        val dispatcher = dispatcherForClient(client)
        URLHandlerRegistry.setDefault(dispatcher)
      },
      update            := update.dependsOn(gitlabHeaderAuthHandler).value,
      updateClassifiers := updateClassifiers.dependsOn(gitlabHeaderAuthHandler).value,
      updateSbtClassifiers := updateSbtClassifiers
        .dependsOn(gitlabHeaderAuthHandler)
        .value,
      publish := publish.dependsOn(gitlabHeaderAuthHandler).value,
      publishTo := (ThisProject / publishTo).value.orElse(
        gitlabProjectId.value
          .map(
            GitlabProjectRepository(gitlabDomain.value, _).resolver
          )
      ),
      gitlabRepositories := Seq.empty,
      resolvers ++= gitlabRepositories.value.map(_.resolver),
      csrConfiguration := addRepositoriesAuthentication(
        csrConfiguration.value,
        gitlabRepositories.value,
        allGitlabCredentials.value
      ),
      updateClassifiers / csrConfiguration := addRepositoriesAuthentication(
        (updateClassifiers / csrConfiguration).value,
        gitlabRepositories.value,
        allGitlabCredentials.value
      ),
      updateSbtClassifiers / csrConfiguration := addRepositoriesAuthentication(
        (updateSbtClassifiers / csrConfiguration).value,
        gitlabRepositories.value,
        allGitlabCredentials.value
      )
    )
}
