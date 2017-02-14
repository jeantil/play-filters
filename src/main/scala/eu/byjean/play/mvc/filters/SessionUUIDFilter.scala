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

import java.time.Instant
import java.util.UUID

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.FiniteDuration

import org.slf4j.MDC
import play.api.mvc._

object SessionUUIDFilter {
  case class SessionId(id: String, expiresAt: Instant)

  val SessionUUIDKey: String = "X-Session-UUID"

  private val DefaultSeparator = ":"
  private val UUID_REGEX = """\p{XDigit}+(?:-\p{XDigit}+){4}"""
  private val SESSION_VALUE_REGEX = s"""($UUID_REGEX):(\\d+)""".r
}

case class SessionUUIDFilter(sessionDuration: FiniteDuration, headerKey: String = SessionUUIDFilter.SessionUUIDKey)(implicit ec: ExecutionContext) extends Filter {
  import SessionUUIDFilter._

  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    val session = updateSessionCookie(requestHeader)
    MDC.put(headerKey, session.id)
    nextFilter(requestHeader).map { result =>
      MDC.remove(headerKey)
      result.withCookies(toCookie(session.id, session.expiresAt))
    }
  }

  private def toCookie(sessionID: String, expiresAt: Instant) = {
    val cookieValue = sessionID + DefaultSeparator + expiresAt.getEpochSecond
    Cookie(headerKey, cookieValue, None, Session.path, Session.domain, Session.secure, Session.httpOnly)
  }

  private def updateSessionCookie(requestHeader: RequestHeader): SessionId = {
    val newExpiration = Instant.now().plusSeconds(sessionDuration.toSeconds)

    extractSession(requestHeader) match {
      case Some(SessionId(sessionID, expiresAt)) if !isExpired(expiresAt) =>
        SessionId(sessionID, newExpiration)
      case _ =>
        SessionId(newSessionID, newExpiration)
    }
  }

  private def isExpired(expiresAt: Instant): Boolean = expiresAt.isBefore(Instant.now())

  private def newSessionID: String = UUID.randomUUID().toString

  private def extractSession(requestHeader: RequestHeader): Option[SessionId] = {
    requestHeader.cookies.get(SessionUUIDKey).map { _.value }
      .orElse(requestHeader.headers.get(SessionUUIDKey))
      .flatMap { parseSessionValue }
  }

  private def parseSessionValue(cookieValue: String): Option[SessionId] = {
    cookieValue match {
      case SESSION_VALUE_REGEX(uuid, timestamp) => Some(SessionId(uuid, Instant.ofEpochSecond(timestamp.toLong)))
      case _ => None
    }
  }

}
