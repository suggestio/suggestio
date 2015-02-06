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

import play.api.Play
import play.api.Play.current
import play.api.i18n.Messages
import play.api.mvc._
import securesocial.core._
import securesocial.core.authenticator.CookieAuthenticator
import securesocial.core.services.SaveMode
import securesocial.core.utils._
import securesocial.util.LoggerImpl

import scala.concurrent.Future

/**
 * A default controller that uses the GenericProfile as the user type
 */
class ProviderController(override implicit val env: RuntimeEnvironment[IProfile])
  extends BaseProviderController[IProfile]

/**
 * A trait that provides the means to authenticate users for web applications
 *
 * @tparam U the user type
 */
trait BaseProviderController[U] extends SecureSocial[U] {
  import securesocial.controllers.ProviderControllerHelper._

  /**
   * The authentication entry point for GET requests
   *
   * @param provider The id of the provider that needs to handle the call
   */
  def authenticate(provider: String, redirectTo: Option[String] = None) = handleAuth(provider, redirectTo)

  /**
   * The authentication entry point for POST requests
   *
   * @param provider The id of the provider that needs to handle the call
   */
  def authenticateByPost(provider: String, redirectTo: Option[String] = None) = handleAuth(provider, redirectTo)

  /**
   * Find the AuthenticatorBuilder needed to start the authenticated session
   */
  protected def builder() = {
    //todo: this should be configurable maybe
    env.authenticatorService.find(CookieAuthenticator.Id).getOrElse {
      logger.error(s"[securesocial] missing CookieAuthenticatorBuilder")
      throw new AuthenticationException()
    }
  }

  /**
   * Common method to handle GET and POST authentication requests
   *
   * @param provider the provider that needs to handle the flow
   * @param redirectTo the url the user needs to be redirected to after being authenticated
   */
  def handleAuth(provider: String, redirectTo: Option[String]) = UserAwareAction.async { implicit request =>
    handleAuth1(provider, redirectTo)
  }
  def handleAuth1(provider: String, redirectTo: Option[String])(implicit request: IRequestWithUser[AnyContent]): Future[Result] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    env.providers.get(provider).map {
      _.authenticate().flatMap {
        case denied: AuthenticationResult.AccessDenied =>
          Future.successful(Redirect(env.routes.loginPageUrl).flashing("error" -> Messages("securesocial.login.accessDenied")))
        case failed: AuthenticationResult.Failed =>
          logger.error(s"[securesocial] authentication failed, reason: ${failed.error}")
          throw new AuthenticationException()
        case flow: AuthenticationResult.NavigationFlow => Future.successful {
          redirectTo.map { url =>
            flow.result.addingToSession(SecureSocial.OriginalUrlKey -> url)
          } getOrElse flow.result
        }
        case authenticated: AuthenticationResult.Authenticated =>
          request.user match {
            case None =>
              val profile = authenticated.profile
              env.userService.find(profile.providerId, profile.userId).flatMap { maybeExisting =>
                val mode = if (maybeExisting.isDefined) SaveMode.LoggedIn else SaveMode.SignUp
                env.userService.save(authenticated.profile, mode).flatMap { userForAction =>
                  import scala.concurrent.ExecutionContext.Implicits.global
                  val autorFut = builder().fromUser(userForAction)
                  logger.debug(s"[securesocial] user completed authentication: provider = ${profile.providerId}, userId: ${profile.userId}, mode = $mode")
                  val evt = if (mode == SaveMode.LoggedIn) new LoginEvent(userForAction) else new SignUpEvent(userForAction)
                  val sessionAfterEvents = Events.fire(evt).getOrElse(request.session)
                  val session1 = cleanupSession(sessionAfterEvents)
                  autorFut.flatMap { authenticator =>
                    Redirect(toUrl(sessionAfterEvents))
                      .withSession(session1)
                      .startingAuthenticator(authenticator)
                  }
                }
              }

            case Some(currentUser) =>
              for (
                linked <- env.userService.link(currentUser, authenticated.profile);
                updatedAuthenticator <- request.authenticator.get.updateUser(linked);
                result <- {
                  val modifiedSession = overrideOriginalUrl(request.session, redirectTo)
                  Redirect(toUrl(modifiedSession))
                    .withSession(cleanupSession(modifiedSession))
                    .touchingAuthenticator(updatedAuthenticator)
                }
              ) yield {
                logger.debug(s"[securesocial] linked $currentUser to: providerId = ${authenticated.profile.providerId}")
                result
              }
          }
      } recover {
        case e =>
          logger.error("Unable to log user in. An exception was thrown", e)
          Redirect(env.routes.loginPageUrl).flashing("error" -> Messages("securesocial.login.errorLoggingIn"))
      }
    } getOrElse {
      Future.successful(NotFound)
    }
  }
}

object ProviderControllerHelper extends LoggerImpl {

  /**
   * The property that specifies the page the user is redirected to if there is no original URL saved in
   * the session.
   */
  def onLoginGoTo = "securesocial.onLoginGoTo"

  /**
   * The root path
   */
  def Root = "/"

  /**
   * The application context
   */
  def ApplicationContext = "application.context"

  /**
   * The url where the user needs to be redirected after succesful authentication.
   *
   * @return
   */
  def landingUrl = Play.configuration.getString(onLoginGoTo).getOrElse(
    Play.configuration.getString(ApplicationContext).getOrElse(Root)
  )

  /**
   * Returns the url that the user should be redirected to after login
   *
   * @param session
   * @return
   */
  def toUrl(session: Session) = session.get(SecureSocial.OriginalUrlKey).getOrElse(ProviderControllerHelper.landingUrl)


  /**
   * Overrides the original url if neded
   *
   * @param session the current session
   * @param redirectTo the url that overrides the originalUrl
   * @return a session updated with the url
   */
  def overrideOriginalUrl(session: Session, redirectTo: Option[String]) = redirectTo match {
    case Some(url) =>
      session + (SecureSocial.OriginalUrlKey -> url)
    case _ =>
      session
  }


  /**
   * Remove securesocial keys from session data.
   * @param s Current session.
   * @return Cleaned session.
   */
  def cleanupSession(s: Session): Session = {
    val filteredKeys = Set(SecureSocial.OriginalUrlKey, IdentityProvider.SessionId, OAuth1Provider.CacheKey)
    s.copy(
      data = s.data.filterKeys { k => !(filteredKeys contains k) }
    )
  }

}
