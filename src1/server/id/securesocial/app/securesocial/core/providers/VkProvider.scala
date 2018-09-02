package securesocial.core.providers

import javax.inject.Inject
import play.api.libs.json.JsObject
import securesocial.core._
import securesocial.core.services.{CacheService, RoutesService}

import scala.concurrent.Future


class VkProviders @Inject() (
                              override val oAuth2SettingsUtil: OAuth2SettingsUtil
                            )
  extends OAuth2ProviderCompanion
{
  override def name = VkProvider.Vk
  override def apply(routesService: RoutesService, cacheService: CacheService, client: OAuth2Client): IdentityProvider = {
    VkProvider(routesService, cacheService, client)
  }
}

object VkProvider {
  val Vk = "vk"

  val GetProfilesApi = "https://api.vk.com/method/getProfiles?fields=uid,first_name,last_name,photo&access_token="
  val Response = "response"
  val Id = "uid"
  val FirstName = "first_name"
  val LastName = "last_name"
  val Photo = "photo"
  val Error = "error"
  val ErrorCode = "error_code"
  val ErrorMessage = "error_msg"

}

import VkProvider._

case class VkProvider(routesService: RoutesService,
  cacheService: CacheService,
  client: OAuth2Client)
    extends OAuth2Provider {

  override def id = VkProvider.Vk

  def fillProfile(info: OAuth2Info): Future[Profile] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val accessToken = info.accessToken
    client.retrieveProfile(GetProfilesApi + accessToken).map { json =>
      (json \ Error).asOpt[JsObject] match {
        case Some(error) =>
          val message = (error \ ErrorMessage).as[String]
          val errorCode = (error \ ErrorCode).as[Int]
          logger.error(
            s"[securesocial] error retrieving profile information from Vk. Error code = $errorCode, message = $message"
          )
          throw new AuthenticationException()
        case _ =>
          val me = (json \ Response).apply(0)
          val userId = (me \ Id).as[Long].toString
          val firstName = (me \ FirstName).asOpt[String]
          val lastName = (me \ LastName).asOpt[String]
          val avatarUrl = (me \ Photo).asOpt[String]
          Profile(id, userId, firstName, lastName, None, None, avatarUrl, authMethod, oAuth2Info = Some(info))
      }
    } recover {
      case e: AuthenticationException => throw e
      case e: Exception =>
        logger.error("[securesocial] error retrieving profile information from VK", e)
        throw new AuthenticationException()
    }
  }
}

