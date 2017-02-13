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

import scala.concurrent.ExecutionContext

import org.joda.time.DateTime
import org.slf4j.MDC
import play.api.libs.Codecs
import play.api.libs.iteratee.Iteratee
import play.api.mvc._

object BrowserUUIDFilter {
  val BrowserUUIDHeaderKey = "X-Browser-UUID"
  val BrowserUUIDCookieKey = "browser_uuid"

  @deprecated("Use BrowserUUIDHeaderKey", "2017-15-02")
  val XBrowserUUID = BrowserUUIDHeaderKey
}

class BrowserUUIDFilter(
    cookieKey: String = BrowserUUIDFilter.BrowserUUIDCookieKey,
    headerKey: String = BrowserUUIDFilter.BrowserUUIDHeaderKey,
    mdcKey: String = BrowserUUIDFilter.BrowserUUIDHeaderKey,
    hashKey: Boolean = false)(
        implicit ec: ExecutionContext) {

  private val hashedkey: String = Codecs.sha1(cookieKey)
  private val key: String = if (hashKey) hashedkey else cookieKey

  def apply(nextFilter: EssentialAction) = new EssentialAction {
    def apply(requestHeader: RequestHeader): Iteratee[Array[Byte], Result] = {
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
    requestHeader.cookies.get(key)
      .orElse(requestHeader.cookies.get(key))
      .map(cookie => cookie.value)

  protected def trackResult(requestHeader: RequestHeader, bid: String)(result: Result): Result = {
    MDC.remove(mdcKey)
    result.withCookies(toCookie(bid))
  }

  private def toCookie(bid: String) = {
    val expiresOn = (DateTime.now().plusYears(5).getMillis / 1000).toInt
    Cookie(key, bid, Some(expiresOn), Session.path, Session.domain, Session.secure, Session.httpOnly)
  }

  private def newId: String = UUID.randomUUID().toString

}

