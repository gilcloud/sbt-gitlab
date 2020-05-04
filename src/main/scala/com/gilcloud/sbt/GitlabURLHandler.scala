package com.gilcloud.sbt

import java.io.{IOException, InputStream}
import java.net.UnknownHostException
import java.util

import gigahorse.support.okhttp.Gigahorse
import okhttp3.internal.http.HttpDate
import okhttp3.{MediaType, OkHttpClient, Request, RequestBody, Response}
import okio.Okio
import org.apache.ivy.util.url.URLHandler.{UNAVAILABLE, URLInfo}
import org.apache.ivy.util.{CopyProgressEvent, CopyProgressListener, Message}
import org.apache.ivy.util.url.{ApacheURLLister, BasicURLHandler, IvyAuthenticator, URLHandler}
import sbt.{File, URL}
import sbt.internal.librarymanagement.ivyint.{ErrorMessageAuthenticator, GigahorseUrlHandler}
import sbt.io.IO

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

object GitlabURLHandlerHelper {
  lazy val http: OkHttpClient = {
    Gigahorse.http(Gigahorse.config)
      .underlying[OkHttpClient]
      .newBuilder()
      .authenticator(new sbt.internal.librarymanagement.JavaNetAuthenticator)
      .followRedirects(true)
      .followSslRedirects(true)
      .build
  }
}


class GitlabURLHandler(headerKey: String, headerValue: String) extends GigahorseUrlHandler(GitlabURLHandlerHelper.http) {

  import GitlabURLHandler._


  private val EmptyBuffer: Array[Byte] = new Array[Byte](0)


  /**
   * Returns the URLInfo of the given url or a #UNAVAILABLE instance,
   * if the url is not reachable.
   */
  override def getURLInfo(url: URL): URLInfo = getURLInfo(url, 0)

  /**
   * Returns the URLInfo of the given url or a #UNAVAILABLE instance,
   * if the url is not reachable.
   */
  override def getURLInfo(url0: URL, timeout: Int): URLInfo = {
    // Install the ErrorMessageAuthenticator
    if (!url0.getHost.contains("gitlab.com")) {
      super.getURLInfo(url0, timeout)

    }
    else {
      if ("http" == url0.getProtocol || "https" == url0.getProtocol) {
        IvyAuthenticator.install()
        ErrorMessageAuthenticator.install()
      }

      val url = normalizeToURL(url0)
      val request = new Request.Builder()
        .url(url)
        .addHeader(headerKey, headerValue)

      if (getRequestMethod == URLHandler.REQUEST_METHOD_HEAD) request.head() else request.get()

      val response = GitlabURLHandlerHelper.http.newCall(request.build()).execute()
      try {
        val infoOption = try {

          if (checkStatusCode(url, response)) {
            val bodyCharset =
              BasicURLHandler.getCharSetFromContentType(
                Option(response.body().contentType()).map(_.toString).orNull
              )
            Some(
              new SbtUrlInfo(
                true,
                response.body().contentLength(),
                lastModifiedTimestamp(response),
                bodyCharset
              )
            )
          } else None

        } catch {
          case e: UnknownHostException =>
            Message.warn("Host " + e.getMessage + " not found. url=" + url)
            Message.info(
              "You probably access the destination server through "
                + "a proxy server that is not well configured."
            )
            None
          case e: IOException =>
            Message.error("Server access Error: " + e.getMessage + " url=" + url)
            None
        }
        infoOption.getOrElse(UNAVAILABLE)
      } finally {
        response.close()
      }
    }
  }

  override def openStream(url: URL): InputStream = {
    if (!url.getHost.contains("gitlab.com")) {
      super.openStream(url)

    }
    else {
      getUrl(url).body().byteStream()

    }

  }

  override def download(url: URL, dest: File, l: CopyProgressListener): Unit  = {
    if (!url.getHost.contains("gitlab.com")) {
      super.download(url, dest, l)
    }
    else {
      val response = getUrl(url)
      try {

        if (l != null) l.start(new CopyProgressEvent())
        val sink = Okio.buffer(Okio.sink(dest))
        try {
          sink.writeAll(response.body().source())
          sink.flush()
        } finally sink.close()
        val contentLength = response.body().contentLength()
        if (contentLength != -1 && dest.length != contentLength) {
          IO.delete(dest)
          throw new IOException(
            "Downloaded file size doesn't match expected Content Length for " + url
              + ". Please retry."
          )
        }

        val lastModified = lastModifiedTimestamp(response)
        if (lastModified > 0) IO.setModifiedTimeOrFalse(dest, lastModified)

        if (l != null) l.end(new CopyProgressEvent(EmptyBuffer, contentLength))

      } finally response.close()
    }
  }


  override def upload(source: File, dest0: URL, l: CopyProgressListener): Unit = {

    if (("http" != dest0.getProtocol) && ("https" != dest0.getProtocol)) throw new UnsupportedOperationException("URL repository only support HTTP PUT at the moment")

    IvyAuthenticator.install()
    ErrorMessageAuthenticator.install()

    val dest = normalizeToURL(dest0)

    val body = RequestBody.create(MediaType.parse("application/octet-stream"), source)

    val request = new Request.Builder()
      .url(dest)
      .addHeader(headerKey, headerValue)
      .addHeader("Content-Type","application/octet-stream")
      .put(body)
      .build()

    if (l != null) l.start(new CopyProgressEvent())
    val response = GitlabURLHandlerHelper.http.newCall(request).execute()
    try {
      if (l != null) l.end(new CopyProgressEvent(EmptyBuffer, source.length()))
      validatePutStatusCode(dest, response.code(), response.message())
    } finally response.close()
  }

  private def getUrl(url0: URL)

  = {
    // Install the ErrorMessageAuthenticator
    if ("http" == url0.getProtocol || "https" == url0.getProtocol) {
      IvyAuthenticator.install()
      ErrorMessageAuthenticator.install()
    }

    val url = normalizeToURL(url0)
    val request = new Request.Builder()
      .url(url)
      .addHeader(headerKey, headerValue)
      .get()
      .build()

    val response = GitlabURLHandlerHelper.http .newCall(request).execute()
    try {
      if (!checkStatusCode(url, response)) throw new IOException(
        "The HTTP response code for " + url + " did not indicate a success."
          + " See log for more detail."
      )
      response
    } catch {
      case NonFatal(e) =>
        //ensure the response gets closed if there's an error
        response.close()
        throw e
    }

  }
}

object GitlabURLHandler {
  // This is requires to access the constructor of URLInfo.
  class SbtUrlInfo(
                    available: Boolean,
                    contentLength: Long,
                    lastModified: Long,
                    bodyCharset: String
                  ) extends URLInfo(available, contentLength, lastModified, bodyCharset) {
    def this(available: Boolean, contentLength: Long, lastModified: Long) = {
      this(available, contentLength, lastModified, null)
    }
  }


  private val EmptyBuffer = new Array[Byte](0)

  private def checkStatusCode(url: URL, response: Response) =
    response.code() match {
      case 200                                          => true
      case 204 if "HEAD" == response.request().method() => true
      case status =>
        Message.debug("HTTP response status: " + status + " url=" + url)
        if (status == 407 /* PROXY_AUTHENTICATION_REQUIRED */ ) Message.warn("Your proxy requires authentication.") else if (status == 401) Message.warn(
          "CLIENT ERROR: 401 Unauthorized. Check your resolvers username and password."
        ) else if (String.valueOf(status).startsWith("4")) Message.verbose("CLIENT ERROR: " + response.message() + " url=" + url) else if (String.valueOf(status).startsWith("5")) Message.error("SERVER ERROR: " + response.message() + " url=" + url)
        false
    }

  private def lastModifiedTimestamp(response: Response): Long = {
    val lastModifiedDate =
      Option(response.headers().get("Last-Modified")).flatMap { headerValue =>
        Option(HttpDate.parse(headerValue))
      }
    lastModifiedDate.map(_.getTime).getOrElse(0)
  }
}

