/**
  * Copyright 2017 Jean Helou
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package eu.byjean.play.mvc.filters

import java.util.UUID
import javax.inject.Inject

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

import akka.stream.Materializer
import akka.util.ByteString
import org.slf4j.MDC
import play.api.Configuration
import play.api.libs.Codecs
import play.api.libs.streams.Accumulator
import play.api.mvc._

object BrowserUUIDFilter {
  val BrowserUUIDDefaultHeaderKey = "X-Browser-UUID"
  val BrowserUUIDDefaultCookieKey = "browser_uuid"
  val BrowserUUIDHeaderKeyConfig  = "eu.byjean.play.filters.browseruuid.headerKey"
  val BrowserUUIDCookieKeyConfig  = "eu.byjean.play.filters.browseruuid.cookieKey"
  val BrowserUUIDMdcKeyConfig     = "eu.byjean.play.filters.browseruuid.mdcKey"
  val BrowserUUIDHashKeyConfig    = "eu.byjean.play.filters.browseruuid.hashKey"
  val BrowserUUIDMaxAgeKeyConfig  = "eu.byjean.play.filters.browseruuid.maxAge"

  @deprecated("Use BrowserUUIDHeaderKey", "2017-15-02")
  val XBrowserUUID: String = BrowserUUIDDefaultHeaderKey
}

class BrowserUUIDFilter @Inject()(config: Configuration, session: SessionCookieBaker)(implicit mat: Materializer, ec: ExecutionContext)
    extends EssentialFilter {

  val cookieKey: String = config
    .get[String](BrowserUUIDFilter.BrowserUUIDCookieKeyConfig)
  val headerKey: String = config
    .get[String](BrowserUUIDFilter.BrowserUUIDHeaderKeyConfig)
  val mdcKey: String = config
    .get[String](BrowserUUIDFilter.BrowserUUIDMdcKeyConfig)
  val hashKey: Boolean = config.get[Boolean](BrowserUUIDFilter.BrowserUUIDHashKeyConfig)

  val maxAge: Duration = config
    .get[Duration](BrowserUUIDFilter.BrowserUUIDMaxAgeKeyConfig)

  private val hashedkey: String = Codecs.sha1(cookieKey)
  private val key: String       = if (hashKey) hashedkey else cookieKey

  override def apply(nextFilter: EssentialAction): EssentialAction = new EssentialAction {
    override def apply(requestHeader: RequestHeader): Accumulator[ByteString, Result] = {
      val tracker: String = getTracker(requestHeader)
      MDC.put(mdcKey, tracker)
      nextFilter(requestHeader).map(trackResult(requestHeader, tracker))
    }
  }

  def getTracker(requestHeader: RequestHeader): String =
    fromCookie(requestHeader)
      .orElse(fromHeader(requestHeader))
      .getOrElse(newId)

  private def fromHeader(requestHeader: RequestHeader): Option[String] =
    requestHeader.headers.get(headerKey)

  private def fromCookie(requestHeader: RequestHeader): Option[String] =
    requestHeader.cookies
      .get(key)
      .orElse(requestHeader.cookies.get(key))
      .map(cookie => cookie.value)

  protected def trackResult(requestHeader: RequestHeader, bid: String)(result: Result): Result = {
    MDC.remove(mdcKey)
    result.withCookies(toCookie(bid))
  }

  private def toCookie(bid: String) = {
    Cookie(key, bid, Some(maxAge.toSeconds.toInt), session.path, session.domain, session.secure, session.httpOnly)
  }

  private def newId: String = UUID.randomUUID().toString

}
