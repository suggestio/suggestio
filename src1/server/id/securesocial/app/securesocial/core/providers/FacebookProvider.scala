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
package securesocial.core.providers

import io.suggest.auth.{AuthenticationException, OAuth2Info, UserProfile}
import javax.inject.Inject
import play.api.libs.json.JsObject
import play.api.libs.ws.WSResponse
import securesocial.core._
import securesocial.core.services.{CacheService, RoutesService}

import scala.concurrent.Future

/**
 * A Facebook Provider
 */

class FacebookProviders @Inject() (
                                    override val oAuth2SettingsUtil: OAuth2SettingsUtil
                                  )
  extends OAuth2ProviderCompanion
{
  override def name = FacebookProvider.Facebook
  override def apply(routesService: RoutesService, cacheService: CacheService, client: OAuth2Client): IdentityProvider = {
    FacebookProvider(routesService, cacheService, client)
  }
}

object FacebookProvider  {
  def Facebook = "facebook"
  def MeApi = "https://graph.facebook.com/me?fields=name,first_name,last_name,picture.type(large),email&return_ssl_resources=1&access_token="
  def Error = "error"
  def Message = "message"
  def Type = "type"
  def Id = "id"
  def FirstName = "first_name"
  def LastName = "last_name"
  def Name = "name"
  def Picture = "picture"
  def Email = "email"
  val AccessToken = "access_token"
  val Expires = "expires"
  def Data = "data"
  def Url = "url"

}

import FacebookProvider._

case class FacebookProvider(routesService: RoutesService,
  cacheService: CacheService,
  client: OAuth2Client)
    extends OAuth2Provider {

  override def id = FacebookProvider.Facebook

  // facebook does not follow the OAuth2 spec :-\
  override protected def buildInfo(response: WSResponse): OAuth2Info = {
    response.body.split("&|=") match {
      case Array(AccessToken, token, Expires, expiresIn) => OAuth2Info(token, None, Some(expiresIn.toInt))
      case Array(AccessToken, token) => OAuth2Info(token)
      case _ =>
        logger.error("[securesocial] invalid response format for accessToken")
        throw new AuthenticationException()
    }
  }

  def fillProfile(info: OAuth2Info): Future[UserProfile] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val accessToken = info.accessToken
    client.retrieveProfile(MeApi + accessToken).map { me =>
      (me \ Error).asOpt[JsObject] match {
        case Some(error) =>
          val message = (error \ Message).as[String]
          val errorType = (error \ Type).as[String]
          logger.error(
            "[securesocial] error retrieving profile information from Facebook. Error type: %s, message: %s".
              format(errorType, message)
          )
          throw new AuthenticationException()
        case _ =>
          val userId = (me \ Id).as[String]
          val name = (me \ Name).asOpt[String]
          val firstName = (me \ FirstName).asOpt[String]
          val lastName = (me \ LastName).asOpt[String]
          val picture = me \ Picture
          val avatarUrl = (picture \ Data \ Url).asOpt[String]
          val email = (me \ Email).asOpt[String]
          UserProfile(id, userId, firstName, lastName, name, email, avatarUrl, authMethod, oAuth2Info = Some(info))
      }
    } recover {
      case e: AuthenticationException => throw e
      case e =>
        logger.error("[securesocial] error retrieving profile information from Facebook", e)
        throw new AuthenticationException()
    }
  }
}

