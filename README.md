# sbt-gitlab

Gitlab dependency resolution and artifact publishing for sbt

```scala
addSbtPlugin("com.gilcloud" % "sbt-gitlab" % "0.0.6") // in your project/plugins.sbt file
```


## Usage

This plugin requires sbt 1.3.0+ as it relies on sbt.internal.CustomHTTP
 
### Dependency Resolution
This plugin also supports dependency resolution from private gitlab package repositories, to use this you need to switch to ivy for dependency resolution rather than the now default coursier as this plugin overrides the handler for ivy. This can be done for example by adding. 
```scala
ThisBuild / useCoursier := false
```

### Publishing to Gitlab via Gitlab CI/CD

Utilizing the sbt publish command within GitLab CI/CD should require no additional configuration. This plugin automatically pulls the following GitLab environment variables which should always be provided by default within GitLab Pipelines

```shell
$CI_JOB_TOKEN   # Access Token to authorize read/writes to the gitlab pakcage registry
$CI_PROJECT_ID  # Project ID for active project pipeline. Used so Gitlab knows what project to publish the artifact under
$CI_GROUP_ID    # Gitlab Groupt ID. Used for fetching Artifacts published under the specified Group. 
                # In a pipeline this would be set to the id of the group the project is under (when applicable)
$CI_SERVER_HOST # The host name for gitlab defaults to gitlab.com
```

Any of these 'defaults' can be overwritten in your build.sbt file

```scala
import com.gilcloud.sbt.gitlab.{GitlabCredentials,GitlabPlugin}

GitlabPlugin.autoImport.gitlabGroupId     :=  Some(12345)
GitlabPlugin.autoImport.gitlabProjectId   :=  Some(12345)
GitlabPlugin.autoImport.gitlabDomain      :=  "my-gitlab-host.com"
GitlabPlugin.autoImport.gitlabCredentials :=  Some(GitlabCredentials("Private-Token","<API-KEY>"))

// Alternatively for credential managment 
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

### Testing

Run `test` for regular unit tests.

Run `scripted` for [sbt script tests](http://www.scala-sbt.org/1.x/docs/Testing-sbt-plugins.html).

### Publishing

1. publish your source to GitHub
2. [create a bintray account](https://bintray.com/signup/index) and [set up bintray credentials](https://github.com/sbt/sbt-bintray#publishing)
3. create a bintray repository `sbt-plugins`
4. update your bintray publishing settings in `build.sbt`
5. `sbt publish`
6. [request inclusion in sbt-plugin-releases](https://bintray.com/sbt/sbt-plugin-releases)
7. [Add your plugin to the community plugins list](https://github.com/sbt/website#attention-plugin-authors)
8. [Claim your project an Scaladex](https://github.com/scalacenter/scaladex-contrib#claim-your-project)
