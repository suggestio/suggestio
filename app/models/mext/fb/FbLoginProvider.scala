package models.mext.fb

import models.mext.ILoginProvider
import securesocial.core.AuthenticationMethod
import securesocial.core.providers.{FacebookProvider, ProviderCompanion}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.04.15 18:39
 * Description: Интеграция facebook и securesocial.
 */
trait FbLoginProvider extends ILoginProvider {
  /** Метод аутентификации SecureSocial. */
  override def ssAuthMethod = AuthenticationMethod.OAuth2

  /** Имя провайдера по мнению SecureSocial. */
  override def ssProvName = FacebookProvider.Facebook

  /** SecureSocial provider companion. */
  override def ssProvider = FacebookProvider
}
