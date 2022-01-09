package nl.zolotko.sbt.gitlab

import okhttp3.{Interceptor, Response}
import sbt.util.Logger

case class HeaderInjector(
    credentials: GitlabCredentials,
    hostMatch: String,
    logger: Logger
) extends Interceptor {
  override def intercept(chain: Interceptor.Chain): Response = {
    val oldReq = chain.request()
    chain.proceed(
      if (oldReq.url.host.contains(hostMatch) && Option(oldReq.headers.get(credentials.key)).isEmpty) {
        logger.debug(s"injecting gitlab token for $oldReq")
        val newReq =
          oldReq.newBuilder().addHeader(credentials.key, credentials.value).build()
        logger.debug(newReq.toString)
        logger.debug(newReq.headers().toString)
        newReq
      } else oldReq
    )
  }
}
