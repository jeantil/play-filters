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

import akka.stream.Materializer
import akka.util.ByteString
import org.slf4j.MDC
import play.api.Configuration
import play.api.libs.streams.Accumulator
import play.api.mvc.{EssentialAction, EssentialFilter, RequestHeader, Result}

object RequestUUIDFilter {
  val XRequestUUIDHeader = "X-Request-UUID"
  val RequestUUIDKey     = "eu.byjean.play.filters.requestuuid.key"
  val RequestUUIDReuse   = "eu.byjean.play.filters.requestuuid.useExisting"

}

class RequestUUIDFilter @Inject()(config: Configuration)(implicit mat: Materializer, ec: ExecutionContext)
    extends EssentialFilter {
  val useExisting: Boolean = config.getBoolean(RequestUUIDFilter.RequestUUIDReuse).getOrElse(true)
  val key: String          = config.getString(RequestUUIDFilter.RequestUUIDKey).getOrElse(RequestUUIDFilter.XRequestUUIDHeader)
  def apply(nextFilter: EssentialAction) = new EssentialAction {
    def apply(requestHeader: RequestHeader): Accumulator[ByteString, Result] = {
      val requestUUID       = readRequestUUID(requestHeader).getOrElse(newRequestUUID)
      val requestUUIDHeader = (key, requestUUID)
      val newHeaders        = requestHeader.headers.add(requestUUIDHeader)
      MDC.put(key, requestUUID)
      nextFilter(requestHeader.copy(headers = newHeaders)).map { result =>
        MDC.remove(key)
        result
      }
    }
  }

  private def readRequestUUID(requestHeader: RequestHeader): Option[String] =
    if (useExisting) {
      requestHeader.headers.get(key)
    } else {
      None
    }

  private def newRequestUUID: String = {
    UUID.randomUUID().toString
  }
}
