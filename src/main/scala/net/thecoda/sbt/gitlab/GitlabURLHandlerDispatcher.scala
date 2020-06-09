package net.thecoda.sbt.gitlab

import gigahorse.support.okhttp.Gigahorse
import okhttp3.{Interceptor, OkHttpClient, Response}
import org.apache.ivy.util.url.{URLHandler, URLHandlerDispatcher}
import sbt.internal.librarymanagement.ivyint.GigahorseUrlHandler

class GitlabURLHandlerDispatcher(
    headerKey: String,
    headerValue: String,
    logger: sbt.util.Logger
) extends URLHandlerDispatcher {

  private[this] class InjectingInterceptor extends Interceptor {
    override def intercept(chain: Interceptor.Chain): Response = {
      val oldReq = chain.request()
      val response = chain.proceed(
        if (oldReq.url.host.contains("gitlab.com")) {
          logger.debug(s"authenticating with gitlab token: $headerKey")
          oldReq
            .newBuilder()
            .addHeader(headerKey, headerValue)
            .build()
        } else oldReq
      )
      if (response.code / 100 == 4) {
        logger.error(response.body.string)
      }
      response
    }
  }

  private[this] def mkHandler(): GigahorseUrlHandler =
    new GigahorseUrlHandler(
      Gigahorse
        .http(Gigahorse.config)
        .underlying[OkHttpClient]
        .newBuilder()
        .authenticator(new sbt.internal.librarymanagement.JavaNetAuthenticator)
        .followRedirects(true)
        .followSslRedirects(true)
        .addNetworkInterceptor(new InjectingInterceptor)
        .build()
    )

  Seq("http", "https") foreach {
    super.setDownloader(_, mkHandler())
  }

  override def setDownloader(
      protocol: String,
      downloader: URLHandler
  ): Unit = {}
}
