# sbt-gitlab

Gitlab dependency resolution and artifact publishing for sbt

```scala
addSbtPlugin("io.github.tangram-flex" % "sbt-gitlab" % "0.0.7") // in your project/plugins.sbt file
```


## Usage

This plugin requires sbt 1.5.0+


### Publishing to Gitlab via Gitlab CI/CD

Utilizing the sbt publish command within GitLab CI/CD should require no additional configuration. This plugin automatically pulls the following GitLab environment variables which should always be provided by default within GitLab Pipelines

```shell
$CI_JOB_TOKEN   # Access Token to authorize read/writes to the gitlab package registry
$CI_PROJECT_ID  # Project ID for active project pipeline. Used so Gitlab knows what project to publish the artifact under
$CI_GROUP_ID    # Gitlab Group ID. Used for fetching Artifacts published under the specified Group. 
                # In a pipeline this would be set to the id of the group the project is under (when applicable)
$CI_SERVER_HOST # The host name for gitlab defaults to gitlab.com
```

Any of these 'defaults' can be overwritten in your build.sbt file

```scala

gitlabGroupId     :=  Some(12345)
gitlabProjectId   :=  Some(12345)
gitlabDomain      :=  "my-gitlab-host.com"
gitlabCredentials :=  Some(GitlabCredentials("Private-Token","<API-KEY>"))  // Not Recommended

// Alternatively for credential management 
// ideal for pulling artifacts locally and keeping tokens out of your source control
// see below for sample .credentials file
credentials += Credentials(Path.userHome / ".sbt" / ".credentials.gitlab"),

```
> ~/.sbt/.credentials.gitlab
```.credentials
realm=gitlab
host=my-git-lab-host
user=Private-Token
password=<API-KEY>
```

### Dependency Resolution

```scala
gitlabGroupId :=  Some(12345)          // The Group ID that your gitlab dependencies reside under
gitlabDomain  :=  "my-gitlab-host.com" // if using a self hosted gitlab instance (only needed for running outside of pipeline)
credentials   += Credentials(Path.userHome / ".sbt" / ".credentials.gitlab") // See sample credential file above (only needed for running outside of pipeline)
```

### Publishing

We rely on GitHub actions & the [sbt-ci-release](https://github.com/sbt/sbt-ci-release) plugin to manage versioning & publishing to Sonatype OSS. See the documentation if you wish to learn more about the configuration.


Additionally, if you wish to know more about Sonatype OSS there is some good information here:
    https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html
