package models.mext.vk

import models.mext.ILoginProvider
import securesocial.core.AuthenticationMethod
import securesocial.core.providers.VkProvider

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.04.15 18:36
 * Description: Реализация интеграции vk и secure-social.
 */
trait VkLoginProvider extends ILoginProvider {

  /** Метод аутентификации SecureSocial. */
  override def ssAuthMethod = AuthenticationMethod.OAuth2

  /** Имя провайдера по мнению SecureSocial. */
  override def ssProvName = VkProvider.Vk

  /** SecureSocial provider companion. */
  override def ssProvider = VkProvider
}
