/**
 * Copyright 2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
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
package securesocial.core.services

import play.api.mvc.{ Call, RequestHeader }
import securesocial.core.IdentityProvider
import securesocial.util.LoggerImpl

/**
 * A RoutesService that resolves the routes for some of the pages
 */
trait RoutesService {

  def authenticationUrl(provider: String, redirectTo: Option[String] = None)(implicit req: RequestHeader): String

  def loginPageUrl(implicit req: RequestHeader): String

}

object RoutesService {
  /**
   * The default RoutesService implementation.  It points to the routes
   * defined by the built in controllers.
   */
  abstract class Default extends RoutesService with LoggerImpl {

    def FaviconKey = "securesocial.faviconPath"
    def JQueryKey = "securesocial.jqueryPath"
    def CustomCssKey = "securesocial.customCssPath"
    def DefaultFaviconPath = "images/favicon.png"
    def DefaultJqueryPath = "javascripts/jquery-1.7.1.min.js"

    protected def absoluteUrl(call: Call)(implicit req: RequestHeader): String = {
      call.absoluteURL(IdentityProvider.sslEnabled)
    }

    def authenticationUrl(provider: String, redirectTo: Option[String] = None)(implicit req: RequestHeader): String

  }
}