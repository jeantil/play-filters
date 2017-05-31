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

import javax.inject.Inject

import controllers.Default
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Filter, RequestHeader, Result}
import scala.concurrent._

import akka.stream.Materializer

class CORSFilter @Inject()(implicit val mat: Materializer) extends Filter {
  lazy val logger: Logger = Logger(this.getClass)

  protected lazy val allowedDomain: Option[String] =
    play.api.Play.current.configuration.getString("cors.allowed.domain")
  protected def isPreFlight(r: RequestHeader): Boolean = (
    r.method.toLowerCase.equals("options")
      &&
        r.headers.get("Access-Control-Request-Method").nonEmpty
  )

  def apply(f: (RequestHeader) => Future[Result])(request: RequestHeader): Future[Result] = {
    if (isPreFlight(request)) {
      if (logger.isTraceEnabled) logger.trace(s"Preflight default allowed domain is '$allowedDomain'")
      Future.successful(
        Default.Ok.withHeaders(
          "Access-Control-Allow-Origin"  -> allowedDomain.orElse(request.headers.get("Origin")).getOrElse(""),
          "Access-Control-Allow-Methods" -> request.headers.get("Access-Control-Request-Method").getOrElse("*"),
          "Access-Control-Allow-Headers" -> request.headers.get("Access-Control-Request-Headers").getOrElse(""),
          "Vary"                         -> "Accept-Encoding, Origin"
        ))
    } else {
      if (logger.isTraceEnabled) logger.trace(s"Default allowed domain is $allowedDomain")
      f(request).map {
        _.withHeaders(
          "Access-Control-Allow-Origin"  -> allowedDomain.orElse(request.headers.get("Origin")).getOrElse(""),
          "Access-Control-Allow-Methods" -> request.headers.get("Access-Control-Request-Method").getOrElse("*"),
          "Access-Control-Allow-Headers" -> request.headers.get("Access-Control-Request-Headers").getOrElse(""),
          "Vary"                         -> "Accept-Encoding, Origin"
        )
      }
    }
  }
}
