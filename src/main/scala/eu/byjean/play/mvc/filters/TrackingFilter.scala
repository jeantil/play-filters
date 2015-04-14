/**
 * Copyright 2015 Jean Helou
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

import org.slf4j.MDC
import play.api.libs.Codecs
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import org.joda.time.DateTime

import scala.collection.parallel
import scala.collection.parallel.immutable

trait TrackingFilter extends EssentialFilter {
  protected def trackResult(requestHeader: RequestHeader, tracker: String)(result: Result): Result
  def trackerKey: String
  def getTracker(requestHeader: RequestHeader): String
  def apply(nextFilter: EssentialAction) = new EssentialAction {
    def apply(requestHeader: RequestHeader) = {
      val tracker: String = getTracker(requestHeader)
      MDC.put(trackerKey, tracker)
      nextFilter(requestHeader).map(trackResult(requestHeader, tracker))
    }
  }
}

object BrowserUUIDFilter {
  val XBrowserUUID = "X-Browser-UUID"

}
case class BrowserUUIDFilter() extends TrackingFilter {
  val BID = Codecs.sha1("browser_uuid")
  private def bidO(requestHeader: RequestHeader): Option[String] = {
    requestHeader.cookies.get(BID).map(cookie => cookie.value)
  }
  private def toCookie(bid: String) = {
    val expiresOn = (DateTime.now().plusYears(5).getMillis / 1000).toInt
    Cookie(BID, bid, Some(expiresOn), Session.path, Session.domain, Session.secure, Session.httpOnly)
  }

  override protected def trackResult(requestHeader: RequestHeader, bid: String)(result: Result): Result = {
    MDC.remove(trackerKey)
    result.withCookies(toCookie(bid))
  }

  override def trackerKey: String = BrowserUUIDFilter.XBrowserUUID

  def newBid: String = UUID.randomUUID().toString

  override def getTracker(requestHeader: RequestHeader): String = bidO(requestHeader).getOrElse(newBid)
}

object RequestUUIDFilter {
  val XRequestUUID = "X-Request-UUID"
}

case class RequestUUIDFilter() extends EssentialFilter {

  def apply(nextFilter: EssentialAction) = new EssentialAction {
    def apply(requestHeader: RequestHeader) = {
      val requestUUID = UUID.randomUUID().toString
      val requestUUIDHeader = (RequestUUIDFilter.XRequestUUID, collection.immutable.Seq(requestUUID))
      val originalHeaders = requestHeader.headers
      val newHeaders = new Headers {
        val data = requestUUIDHeader +: originalHeaders.toMap.toSeq
      }
      MDC.put(RequestUUIDFilter.XRequestUUID, requestUUID)
      nextFilter(requestHeader.copy(headers = newHeaders)).map { result =>
        MDC.remove(RequestUUIDFilter.XRequestUUID)
        result
      }
    }
  }
}
