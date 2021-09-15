package nl.zolotko.sbt.gitlab

import sbt.DirectCredentials

case class GitlabCredentials(key: String, value: String)

object GitlabCredentials {

  /** Apply method for Private-Token or Job-Token supplied by sbt credentials blob
    * @param directCredentials credentials class
    * @return GitlabCredentials
    */
  def apply(directCredentials: DirectCredentials): GitlabCredentials =
    GitlabCredentials(directCredentials.userName, directCredentials.passwd)

}
