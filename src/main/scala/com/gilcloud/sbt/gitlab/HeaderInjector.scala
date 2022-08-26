package com.gilcloud.sbt.gitlab

//import okhttp3.{Interceptor, Response}
//import sbt.util.Logger
//
//case class HeaderInjector(
//    creds: GitlabCredentials,
//    hostMatch: String,
//    optLogger: Option[Logger] = None
//) extends Interceptor {
//  def logInfo(str: => String): Unit  = optLogger.foreach(_.info(str))
//  def logDebug(str: => String): Unit = optLogger.foreach(_.debug(str))
//
//  override def intercept(chain: Interceptor.Chain): Response = {
//    val oldReq = chain.request()
//    chain.proceed(
//      if (oldReq.url.host.contains(hostMatch) && Option(oldReq.headers.get(creds.key)).isEmpty) {
//        logDebug(s"injecting gitlab token for $oldReq")
//        val newReq =
//          oldReq.newBuilder().addHeader(creds.key, creds.value).build()
//        logDebug(newReq.toString)
//        logDebug(newReq.headers().toString)
//        newReq
//      } else oldReq
//    )
//  }
//}
