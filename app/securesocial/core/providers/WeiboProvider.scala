/**
 * Copyright 2013 wuhaixing (wuhaixing at gmail dot com) - weibo: @数据水墨
 *                qiuzhanghua (qiuzhanghua at gmail.com) - weibo: qiuzhanghua
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
 *
 */
package securesocial.core.providers

import play.api.libs.ws.{ WS, WSResponse }
import securesocial.core._
import securesocial.core.services.{ CacheService, RoutesService }

import scala.concurrent.{ ExecutionContext, Future }

/**
 * A Weibo provider
 */

object WeiboProvider extends OAuth2ProviderCompanion {
  val Weibo = "weibo"
  override def name = Weibo

  val GetAuthenticatedUser = "https://api.weibo.com/2/users/show.json?uid=%s&access_token=%s"
  val AccessToken = "access_token"
  val Message = "error"
  val UId = "uid"
  val Id = "idstr"
  val Name = "name"
  val AvatarUrl = "profile_image_url"
  val GetUserEmail = "https://api.weibo.com/2/account/profile/email.json?access_token=%s"
  val Email = "email"

}

import WeiboProvider._

case class WeiboProvider(routesService: RoutesService,
  cacheService: CacheService,
  client: OAuth2Client)
    extends OAuth2Provider {

  override def id = WeiboProvider.Weibo

  /**
   *
   * According to the weibo.com's OAuth2 implemention,I use TokenType position place UId param
   * So please check http://open.weibo.com/wiki/OAuth2/access_token to ensure they stay weird
   * before you use this.
   *
   */
  override protected def buildInfo(response: WSResponse): OAuth2Info = {
    val json = response.json
    logger.debug("[securesocial] got json back [" + json + "]")
    //UId occupied TokenType in the weibo.com provider
    OAuth2Info(
      (json \ OAuth2Constants.AccessToken).as[String],
      (json \ UId).asOpt[String],
      (json \ OAuth2Constants.ExpiresIn).asOpt[Int],
      (json \ OAuth2Constants.RefreshToken).asOpt[String]
    )
  }

  /**
   * Subclasses need to implement this method to populate the User object with profile
   * information from the service provider.
   *
   * @param info The OAuth2Info
   * @return A copy of the user object with the new values set
   */
  def fillProfile(info: OAuth2Info): Future[Profile] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val accessToken = info.accessToken
    val weiboUserId = info.tokenType.getOrElse {
      logger.error("[securesocial] Can't found weiboUserId")
      throw new AuthenticationException()
    }

    client.retrieveProfile(GetAuthenticatedUser.format(weiboUserId, accessToken)).flatMap { me =>
      (me \ Message).asOpt[String] match {
        case Some(msg) =>
          logger.error("[securesocial] error retrieving profile information from Weibo. Message = %s".format(msg))
          throw new AuthenticationException()
        case _ =>
          val userId = (me \ Id).as[String]
          val displayName = (me \ Name).asOpt[String]
          val avatarUrl = (me \ AvatarUrl).asOpt[String]
          getEmail(accessToken).map { email =>
            Profile(id, userId, None, None, displayName, email, avatarUrl, authMethod, None, Some(info))
          }
      }
    } recover {
      case e =>
        logger.error("[securesocial] error retrieving profile information from weibo", e)
        throw new AuthenticationException()
    }
  }

  def getEmail(accessToken: String)(implicit ec: ExecutionContext): Future[Option[String]] = {
    import play.api.Play.current
    WS.url(GetUserEmail.format(accessToken)).get().map { response =>
      val me = response.json
      (me \ Message).asOpt[String] match {
        case Some(msg) =>
          logger.error("[securesocial] error retrieving email information from Weibo. Message = %s".format(msg))
          None
        case _ =>
          (me \ Email).asOpt[String].filter(!_.isEmpty)
      }
    } recover {
      case e: Exception =>
        logger.error("[securesocial] error retrieving profile information from weibo", e)
        None
    }
  }

}
