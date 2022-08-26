package com.gilcloud.sbt.gitlab

//import coursier.core.Versions.DateTime
//import gigahorse.FullResponse
//import gigahorse.support.apachehttp.{ApacheHttpClient, Gigahorse}
import org.apache.ivy.Ivy
import org.apache.ivy.util.FileUtil
import org.apache.ivy.util.url.BasicURLHandler.{HttpStatus, getCharSetFromContentType}
import org.apache.ivy.util.url.URLHandler

import java.net.URLDecoder
//import sbt.internal.librarymanagement.ivyint.ErrorMessageAuthenticator
//import sbt.librarymanagement.Http.http

import java.net.{HttpURLConnection, URL, URLConnection, UnknownHostException}
import java.io.*
import java.util
import scala.collection.mutable
//import scala.util.control.NonFatal

//import okhttp3.{ MediaType, Request, RequestBody }
//import okhttp3.internal.http.HttpDate
//
//import okhttp3.{ JavaNetAuthenticator => _, _ }
//import okio._

import org.apache.ivy.util.{ CopyProgressEvent, CopyProgressListener, Message }
import org.apache.ivy.util.url.{ AbstractURLHandler, BasicURLHandler, IvyAuthenticator, URLHandler }
import org.apache.ivy.util.url.URLHandler._
import sbt.io.IO

case class GitlabUrlHandler(credentials: GitlabCredentials) extends BasicURLHandler {
  private val BUFFER_SIZE = 64 * 1024
  private val ERROR_BODY_TRUNCATE_LEN = 512

  private object HttpStatus {
    private[GitlabUrlHandler] val SC_OK = 200
    private[GitlabUrlHandler] val SC_PROXY_AUTHENTICATION_REQUIRED = 407
  }

    // This is requires to access the constructor of URLInfo.
    private[gitlab] class SbtUrlInfo(
       available: Boolean,
       contentLength: Long,
       lastModified: Long,
       bodyCharset: String
     ) extends URLInfo(available, contentLength, lastModified, bodyCharset) {
      def this(available: Boolean, contentLength: Long, lastModified: Long) = {
        this(available, contentLength, lastModified, null)
      }
    }


  private def getHeadersAsDebugString(headers: util.Map[String, util.List[String]]) = {
    val builder = new mutable.StringBuilder("")
    if (headers != null) {
      import scala.collection.JavaConversions._
      for (header <- headers.entrySet) {
        val key = header.getKey
        if (key != null) {
          builder.append(header.getKey)
          builder.append(": ")
        }
        builder.append(String.join("\n    ", header.getValue))
        builder.append("\n")
      }
    }
    builder.toString
  }

  private def readResponseBody(conn: HttpURLConnection): Unit = {
    val buffer = new Array[Byte](BUFFER_SIZE)
    var inStream: InputStream = null
    try {
      inStream = conn.getInputStream
      while ( {
        inStream.read(buffer) > 0
      }) {
        //Skip content
      }
    } catch {
      case e: IOException =>

      // ignore
    } finally if (inStream != null) try inStream.close()
    catch {
      case e: IOException =>

    }
    val errStream = conn.getErrorStream
    if (errStream != null) try while ( {
      errStream.read(buffer) > 0
    }) {
    }
    catch {
      case e: IOException =>

    } finally try errStream.close()
    catch {
      case e: IOException =>

    }
  }

  private def disconnect(con: URLConnection): Unit = {
    if (con.isInstanceOf[HttpURLConnection]) {
      if (!("HEAD" == con.asInstanceOf[HttpURLConnection].getRequestMethod)) { // We must read the response body before disconnecting!
        // Cfr. http://java.sun.com/j2se/1.5.0/docs/guide/net/http-keepalive.html
        // [quote]Do not abandon a connection by ignoring the response body. Doing
        // so may results in idle TCP connections.[/quote]
        readResponseBody(con.asInstanceOf[HttpURLConnection])
      }
      con.asInstanceOf[HttpURLConnection].disconnect()
    }
    else if (con != null) try con.getInputStream.close()
    catch {
      case e: IOException =>

      // ignored
    }
  }

  @throws[IOException]
  private def readTruncated(is: InputStream, maxLen: Int, contentType: String, contentEncoding: String) = {
    val decodingStream = getDecodingInputStream(contentEncoding, is)
    val charSet = getCharSetFromContentType(contentType)
    val os = new ByteArrayOutputStream(maxLen)
    try {
      var count = 0
      var b = decodingStream.read
      while ( {
        count < maxLen && b >= 0
      }) {
        os.write(b)
        count += 1
        b = decodingStream.read
      }
      new String(os.toByteArray, charSet)
    } finally try is.close()
    catch {
      case e: IOException =>

      /* ignored */
    }
  }

  @throws[IOException]
  private def checkStatusCode(url: URL, con: HttpURLConnection): Boolean = {
    val status = con.getResponseCode
    if (status == HttpStatus.SC_OK) return true
    // IVY-1328: some servers return a 204 on a HEAD request
    if ("HEAD" == con.getRequestMethod && (status == 204)) return true
    Message.debug("HTTP response status: " + status + " url=" + url)
    if (status == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED) Message.warn("Your proxy requires authentication.")
    else if (String.valueOf(status).startsWith("4")) Message.verbose("CLIENT ERROR: " + con.getResponseMessage + " url=" + url)
    else if (String.valueOf(status).startsWith("5")) Message.error("SERVER ERROR: " + con.getResponseMessage + " url=" + url)
    false
  }
  override def getURLInfo(url0: URL, timeout: Int): URLInfo = {
    println(s"getUrlInfo for $url0 timeout $timeout") // Install the IvyAuthenticator
    if ("http" == url0.getProtocol || "https" == url0.getProtocol) IvyAuthenticator.install()

    var con: URLConnection = null

    try {
      val url = normalizeToURL(url0)
      con = url.openConnection
      con.setRequestProperty("User-Agent", "Apache Ivy/" + Ivy.getIvyVersion)
      con.setRequestProperty(credentials.key, credentials.value)
      if (con.isInstanceOf[HttpURLConnection]) {
        val httpCon = con.asInstanceOf[HttpURLConnection]
        if (getRequestMethod == URLHandler.REQUEST_METHOD_HEAD) httpCon.setRequestMethod("HEAD")
        if (checkStatusCode(url, httpCon)) {
          val bodyCharset = getCharSetFromContentType(con.getContentType)
          return new SbtUrlInfo(true, httpCon.getContentLength, con.getLastModified, bodyCharset)
        }
      }
      else {
        val contentLength = con.getContentLength
        if (contentLength <= 0) return UNAVAILABLE
        else { // TODO: not HTTP... maybe we *don't* want to default to ISO-8559-1 here?
          val bodyCharset = getCharSetFromContentType(con.getContentType)
          return new SbtUrlInfo(true, contentLength, con.getLastModified, bodyCharset)
        }
      }
    } catch {
      case e: UnknownHostException =>
        Message.warn("Host " + e.getMessage + " not found. url=" + url0)
        Message.info("You probably access the destination server through " + "a proxy server that is not well configured.")
      case e: IOException =>
        Message.error("Server access Error: " + e.getMessage + " url=" + url0)
    } finally disconnect(con)
    return UNAVAILABLE
  }
  override def download(src0: URL, dest: File, l: CopyProgressListener): Unit = {
    println(s"Download request from $src0")
    // Install the IvyAuthenticator
    if ("http" == src0.getProtocol || "https" == src0.getProtocol) IvyAuthenticator.install()

    var srcConn: URLConnection = null
    try {
      val src = normalizeToURL(src0)
      srcConn = src.openConnection
      srcConn.setRequestProperty("User-Agent", "Apache Ivy/" + Ivy.getIvyVersion)
      srcConn.setRequestProperty("Accept-Encoding", "gzip,deflate")
      srcConn.setRequestProperty("Accept", "application/octet-stream, application/json, application/xml, */*")
      srcConn.setRequestProperty(credentials.key, credentials.value)
      if (srcConn.isInstanceOf[HttpURLConnection]) {
        val httpCon = srcConn.asInstanceOf[HttpURLConnection]
        val status = httpCon.getResponseCode
        if (status == 302 || status == 301) {
          var location = httpCon.getHeaderField("Location")
          location = URLDecoder.decode(location, "UTF-8")
          val next = new URL(location)
          download(next, dest, l)
          disconnect(srcConn)
          return
        }
        else if (!checkStatusCode(src, httpCon)) throw new IOException("The HTTP response code for " + src + " did not indicate a success." + " See log for more detail.")
      }
      // do the download
      val inStream = getDecodingInputStream(srcConn.getContentEncoding, srcConn.getInputStream)
      FileUtil.copy(inStream, dest, l)
      // check content length only if content was not encoded
      if (srcConn.getContentEncoding == null) {
        val contentLength = srcConn.getContentLength
        if (contentLength != -1 && dest.length != contentLength) {
          dest.delete
          throw new IOException("Downloaded file size doesn't match expected Content Length for " + src + ". Please retry.")
        }
      }
      // update modification date
      val lastModified = srcConn.getLastModified
      if (lastModified > 0) dest.setLastModified(lastModified)
    } finally disconnect(srcConn)
  }

  override def upload(source: File, dest0: URL, l: CopyProgressListener): Unit = {
    if (!("http" == dest0.getProtocol) && !("https" == dest0.getProtocol)) throw new UnsupportedOperationException("URL repository only support HTTP PUT at the moment")

    // Install the IvyAuthenticator
    IvyAuthenticator.install()
    var conn: HttpURLConnection = null
    val dest = normalizeToURL(dest0)
    try {

      conn = dest.openConnection.asInstanceOf[HttpURLConnection]
      conn.setDoOutput(true)
      conn.setRequestMethod("PUT")
      conn.setRequestProperty("User-Agent", "Apache Ivy/" + Ivy.getIvyVersion)
      conn.setRequestProperty("Accept", "application/octet-stream, application/json, application/xml, */*")
      conn.setRequestProperty("Content-type", "application/octet-stream")
      conn.setRequestProperty("Content-length", source.length().toString)
      conn.setRequestProperty(credentials.key, credentials.value)
      conn.setInstanceFollowRedirects(true)
      Message.debug("Request Headers:" + getHeadersAsDebugString(conn.getRequestProperties))
      val in = new FileInputStream(source)
      val os = conn.getOutputStream
      FileUtil.copy(in, os, l)
      in.close()

      val i = 1
      // initiate the connection
      val responseCode = conn.getResponseCode
      var extra = ""
      val errorStream = conn.getErrorStream
      val responseStream = conn.getInputStream
      if (errorStream != null) extra = "; Response Body: " + readTruncated(errorStream, ERROR_BODY_TRUNCATE_LEN, conn.getContentType, conn.getContentEncoding)
      else if (responseStream != null) {
        val decodingStream = getDecodingInputStream(conn.getContentEncoding, responseStream)
        extra = "; Response Body: " + readTruncated(responseStream, ERROR_BODY_TRUNCATE_LEN, conn.getContentType, conn.getContentEncoding)
      }
      Message.debug("Response Headers:" + getHeadersAsDebugString(conn.getHeaderFields))
      validatePutStatusCode(dest, responseCode, conn.getResponseMessage + extra)
    } catch {
      case e: IOException =>

      /* ignored */
    } finally disconnect(conn)
  }
}

