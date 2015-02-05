package controllers.ident

import controllers.SioController
import models.{ExternalCall, Context}
import models.usr.{SsUserService, SsUser}
import play.api.mvc.{Result, AnyContent, Call, RequestHeader}
import securesocial.controllers._
import securesocial.core.RuntimeEnvironment.Default
import securesocial.core.providers.VkProvider
import securesocial.core.services.{RoutesService, UserService}
import securesocial.core.{IdentityProvider, RuntimeEnvironment, Profile}

import scala.collection.immutable.ListMap
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.01.15 17:16
 * Description: Поддержка логина через соц.сети или иные внешние сервисы.
 */

trait ExternalLogin extends SioController with BaseProviderController[SsUser] {

  /** secure-social настраивается через этот Enviroment. */
  override implicit val env: RuntimeEnvironment[SsUser] = {
    new Default[SsUser] {
      override lazy val routes: RoutesService = SsRoutesService
      override def userService: UserService[SsUser] = SsUserService
      override lazy val providers: ListMap[String, IdentityProvider] = {
        ListMap(
          include(new VkProvider(routes, cacheService, oauth2ClientFor(VkProvider.Vk)))
        )
      }
    }
  }

}

object SsRoutesService extends RoutesService.Default {

  override def absoluteUrl(call: Call)(implicit req: RequestHeader): String = {
    if(call.isInstanceOf[ExternalCall])
      call.url
    else
      Context.LK_URL_PREFIX + call.url
  }

  override def authenticationUrl(provider: String, redirectTo: Option[String])(implicit req: RequestHeader): String = {
    val relUrl = controllers.routes.Ident.authenticate(provider, redirectTo)
    absoluteUrl(relUrl)
  }

}
