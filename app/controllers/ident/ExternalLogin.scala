package controllers.ident

import controllers.SioController
import securesocial.controllers._
import securesocial.core.RuntimeEnvironment.Default
import securesocial.core.providers.VkProvider
import securesocial.core.services.UserService
import securesocial.core.{IdentityProvider, RuntimeEnvironment, BasicProfile}

import scala.collection.immutable.ListMap

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.01.15 17:16
 * Description: Поддержка логина через соц.сети или иные внешние сервисы.
 */
trait ExternalLogin extends SioController with BaseProviderController[BasicProfile] {

  /** secure-social настраивается через этот Enviroment. */
  override implicit val env: RuntimeEnvironment[BasicProfile] = {
    new Default[BasicProfile] {
      override def userService: UserService[BasicProfile] = ???   // TODO Нужно модель создать, скрестить её с необходимым API.
      override lazy val providers: ListMap[String, IdentityProvider] = {
        ListMap(
          include(new VkProvider(routes, cacheService, oauth2ClientFor(VkProvider.Vk)))
        )
      }
    }
  }

}
