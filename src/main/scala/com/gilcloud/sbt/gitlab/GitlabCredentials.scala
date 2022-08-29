package com.gilcloud.sbt.gitlab

import sbt.DirectCredentials

case class GitlabCredentials(host: String, key: String, value: String)

object GitlabCredentials {

  /**
    * Apply method for Private-Token or Job-Token supplied by sbt credentials blob
    * @param directCredentials credentials class
    * @return GitlabCredentials
    */
  def apply(host:String, directCredentials: DirectCredentials): GitlabCredentials =
    GitlabCredentials(host, directCredentials.userName, directCredentials.passwd)

}
