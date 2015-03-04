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

import play.api.libs.Codecs
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import org.joda.time.DateTime

import scala.collection.parallel
import scala.collection.parallel.immutable

trait TrackingFilter extends EssentialFilter {
  protected def trackResult(requestHeader: RequestHeader)(result: Result): Result

  def apply(nextFilter: EssentialAction) = new EssentialAction {
    def apply(requestHeader: RequestHeader) = {
      nextFilter(requestHeader).map(trackResult(requestHeader))
    }
  }
}

case class BrowserUUIDFilter() extends TrackingFilter {
  val BID = Codecs.sha1("browser_uuid")
  private def bidO(requestHeader: RequestHeader): Option[String] = {
    requestHeader.cookies.get(BID).map(cookie => cookie.value)
  }
  private def createBid() = {
    val expiresOn = (DateTime.now().plusYears(5).getMillis / 1000).toInt
    Cookie(BID, UUID.randomUUID().toString, Some(expiresOn), Session.path, Session.domain, Session.secure, Session.httpOnly)
  }
  override protected def trackResult(requestHeader: RequestHeader)(result: Result): Result = {
    result.withCookies(requestHeader.cookies.get(BID).getOrElse(createBid()))
  }
}
object RequestUUIDFilter {
  val XRequestUUID = "X-Request-UUID"
}
case class RequestUUIDFilter() extends EssentialFilter {

  def apply(nextFilter: EssentialAction) = new EssentialAction {
    def apply(requestHeader: RequestHeader) = {
      val requestUUIDHeader = (RequestUUIDFilter.XRequestUUID, collection.immutable.Seq(UUID.randomUUID().toString))
      val originalHeaders = requestHeader.headers
      val newHeaders = new Headers {
        val data = requestUUIDHeader +: originalHeaders.toMap.toSeq
      }
      nextFilter(requestHeader.copy(headers = newHeaders))
    }
  }
}
