/**
 * Copyright 2012-2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package securesocial.controllers

import java.time.OffsetDateTime
import java.util.UUID

import play.api.Play
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.i18n.Messages
import play.api.mvc.{RequestHeader, Result}
import securesocial.core.SecureSocial
import securesocial.core.providers.MailToken

import scala.concurrent.Future

/**
 * The base controller for password reset and password change operations
 *
 */
abstract class MailTokenBasedOperations[U] extends SecureSocial[U] {
  val Success = "success"
  val Error = "error"
  val Email = "email"
  val TokenDurationKey = "securesocial.userpass.tokenDuration"
  val DefaultDuration = 60
  val TokenDuration = Play.current.configuration.getInt(TokenDurationKey).getOrElse(DefaultDuration)

  val startForm = Form(
    Email -> email.verifying(nonEmpty)
  )

  /**
   * Creates a token for mail based operations
   *
   * @param email the email address
   * @param isSignUp a boolean indicating if the token is used for a signup or password reset operation
   * @return a MailToken instance
   */
  def createToken(email: String, isSignUp: Boolean): Future[MailToken] = {
    val now = OffsetDateTime.now()

    Future.successful(MailToken(
      UUID.randomUUID().toString, email.toLowerCase, now, now.plusMinutes(TokenDuration), isSignUp = isSignUp
    ))
  }

  /**
   * Helper method to execute actions where a token needs to be retrieved from
   * the backing store
   *
   * @param token the token id
   * @param isSignUp a boolean indicating if the token is used for a signup or password reset operation
   * @param f the function that gets invoked if the token exists
   * @param request the current request
   * @return the action result
   */
  protected def executeForToken(token: String, isSignUp: Boolean,
    f: MailToken => Future[Result])(implicit request: RequestHeader): Future[Result] =
    {
      import scala.concurrent.ExecutionContext.Implicits.global
      env.userService.findToken(token).flatMap {
        case Some(t) if !t.isExpired && t.isSignUp == isSignUp => f(t)
        case _ =>
          val to = if (isSignUp) env.routes.signUpUrl else env.routes.resetPasswordUrl
          Future.successful(Redirect(to).flashing(Error -> Messages(BaseRegistration.InvalidLink)))
      }
    }

  /**
   * The result sent after the start page is handled
   *
   * @param request the current request
   * @return the action result
   */
  protected def handleStartResult()(implicit request: RequestHeader): Result = Redirect(env.routes.loginPageUrl)

  /**
   * The result sent after the operation has been completed by the user
   *
   * @param request the current request
   * @return the action result
   */
  protected def confirmationResult()(implicit request: RequestHeader): Result = Redirect(env.routes.loginPageUrl)
}
