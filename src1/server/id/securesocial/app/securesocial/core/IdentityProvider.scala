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

import io.suggest.auth.{AuthenticationMethod, AuthenticationResult}
import javax.inject.Inject
import play.api.mvc.{AnyContent, Request}
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

  def SessionId = "sid"

  def throwMissingPropertiesException(id: String) {
    val msg = s"[securesocial] Missing properties for provider '$id'. Verify your configuration file is properly set."
    logger.error(msg)
    throw new RuntimeException(msg)
  }
}

