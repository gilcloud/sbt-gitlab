package com.gilcloud.sbt

import sbt.Keys._
import sbt.{Def, _}
import org.apache.ivy.util.url._
import sbt.librarymanagement.ivy.Credentials


object GitlabPlugin extends AutoPlugin {

  lazy val headerAuth = taskKey[Unit]("")
  // This plugin will load automatically
  override def trigger: PluginTrigger = allRequirements
  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    publishMavenStyle := true,
    headerAuth := {
      val cred = (credentials).value
      val filteredCred = cred.filter{
        case f : FileCredentials => f.path.exists()
        case _ => true
      }
      val creds = Credentials.allDirect(filteredCred)
      creds.find(_.realm == "gitlab") match {
        case Some(cred) =>
          val urlHandlerDispatcher = new URLHandlerDispatcher {
            super.setDownloader("https", new GitlabURLHandler(cred.userName,cred.passwd))
            super.setDownloader("http", new GitlabURLHandler(cred.userName,cred.passwd))
            override def setDownloader(protocol: String, downloader: URLHandler): Unit = {}
          }
          URLHandlerRegistry.setDefault(urlHandlerDispatcher)
        case None =>
      }
      creds.filter(_.realm != "gitlab")
    },
    update := update.dependsOn(headerAuth).value,
    updateClassifiers := updateClassifiers.dependsOn(headerAuth).value,
    updateSbtClassifiers := updateSbtClassifiers.dependsOn(headerAuth).value,
    publish := publish.dependsOn(headerAuth).value)
}
