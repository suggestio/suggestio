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
package securesocial.core

import javax.inject.Inject
import play.api.mvc.{AnyContent, Request, Result}
import play.api.Configuration
import securesocial.util.LazyLoggerImpl

import concurrent.Future

/**
 * Base class for all Identity Providers.
 */
trait IdentityProvider {
  /**
   * The id for this provider.
   */
  def id: String

  /**
   * Subclasses need to implement this to specify the authentication method
   * @return
   */
  def authMethod: AuthenticationMethod

  /**
   * Returns the provider name
   *
   * @return
   */
  override def toString = id

  /**
   * Authenticates the user and fills the profile information.
   *
   * @param request the current request
   * @return a future AuthenticationResult
   */
  def authenticate()(implicit request: Request[AnyContent]): Future[AuthenticationResult]
}

class IdentityProviders @Inject() (configuration: Configuration) {

  /**
   * Reads a property from the application.conf
   * @param property
   * @return
   */
  def loadProperty(providerId: String, property: String, optional: Boolean = false): Option[String] = {
    val key = s"securesocial.$providerId.$property"
    val result = configuration.getOptional[String](key)
    if (!result.isDefined && !optional) {
      IdentityProvider.logger.warn(s"[securesocial] Missing property: $key ")
    }
    result
  }

}

object IdentityProvider extends LazyLoggerImpl {

  val SessionId = "sid"

  def throwMissingPropertiesException(id: String) {
    val msg = s"[securesocial] Missing properties for provider '$id'. Verify your configuration file is properly set."
    logger.error(msg)
    throw new RuntimeException(msg)
  }
}

/**
 * An object that represents the different results of the authentication flow
 */
sealed trait AuthenticationResult

object AuthenticationResult {
  /**
   * A user denied access to their account while authenticating with an external provider (eg: Twitter)
   */
  case class AccessDenied() extends AuthenticationResult

  /**
   * An intermetiate result during the authentication flow (maybe a redirection to the external provider page)
   */
  case class NavigationFlow(result: Result) extends AuthenticationResult

  /**
   * Returned when the user was succesfully authenticated
   * @param profile the authenticated user profile
   */
  case class Authenticated(profile: Profile) extends AuthenticationResult

  /**
   * Returned when the authentication process failed for some reason.
   * @param error a description of the error
   */
  case class Failed(error: String) extends AuthenticationResult
}

/**
 * This traits enables providers to be used by the LoginApi controller.
 *
 * @see LoginApi
 */
trait ApiSupport {
  /**
   * Authenticates a user
   * @param request
   * @return
   */
  def authenticateForApi(implicit request: Request[AnyContent]): Future[AuthenticationResult]
}

