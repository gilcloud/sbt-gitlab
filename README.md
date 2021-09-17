# sbt-gitlab

GitLab dependency resolution and artifact publishing for sbt.

## Requirements

This plugin requires sbt 1.5.0+ as it relies on Coursier and sbt.internal.CustomHTTP

## Usage

Add the following to your project/plugins.sbt file:

```scala
resolvers += "Sonatype OSS" at "https://s01.oss.sonatype.org/content/repositories/public"
addSbtPlugin("nl.zolotko.sbt" % "sbt-gitlab" % "0.0.7")
```

And then configure the plugin in your build.sbt file by overriding gitlabDomain (default is gitlab.com) and
gitlabGroupId/gitlabProjectId, for example:

```scala
gitlabDomain := "gitlab.your-company.com"
gitlabGroupId := Some(13)
```

### Credentials

You can either put your credentials directly into build.sbt (not recommended):

```scala
import nl.zolotko.sbt.gitlab.GitlabCredentials
gitlabCredentials := Some(GitlabCredentials("Private-Token", "<ACCESS-TOKEN>"))
```

Or, keep them out of your source control:

> ~/.sbt/.credentials.gitlab

```.credentials
realm=gitlab
host=my-git-lab-host
user=Private-Token
password=<API-KEY>
```

> build.sbt or ~/.sbt/1.0/credentials.sbt

```scala 
credentials += Credentials(Path.userHome / ".sbt" / ".credentials.gitlab")
```

### Dependency Resolution

This plugin also supports dependency resolution from private gitlab package repositories.

### Publishing to Gitlab via Gitlab CI/CD

Utilizing the sbt publish command within GitLab CI/CD should require no additional configuration. This plugin
automatically pulls the following GitLab environment variables which should always be provided by default within GitLab
Pipelines

```shell
$CI_JOB_TOKEN   # Access Token to authorize read/writes to the gitlab package registry
$CI_PROJECT_ID  # Project ID for active project pipeline. Used so Gitlab knows what project to publish the artifact under
$CI_GROUP_ID    # Gitlab Group ID. Used for fetching Artifacts published under the specified Group. 
                # In a pipeline this would be set to the id of the group the project is under (when applicable)
$CI_SERVER_HOST # The host name for gitlab defaults to gitlab.com
```

### Testing

Run `test` for regular unit tests.

Run `scripted` for [sbt script tests](http://www.scala-sbt.org/1.x/docs/Testing-sbt-plugins.html).

### Credits

This plugin a fork of [gilcloud/sbt-gitlab](https://github.com/gilcloud/sbt-gitlab).
