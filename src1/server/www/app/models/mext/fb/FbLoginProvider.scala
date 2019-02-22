package models.mext.fb

import models.mext.ILoginProvider
import securesocial.core.providers.{FacebookProvider, FacebookProviders}

import scala.reflect.ClassTag

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.04.15 18:39
 * Description: Интеграция facebook и securesocial.
 */
trait FbLoginProvider extends ILoginProvider {

  /** Имя провайдера по мнению SecureSocial. */
  override def ssProvName = FacebookProvider.Facebook

  /** SecureSocial provider companion. */
  override def ssProviderClass = ClassTag( classOf[FacebookProviders] )

}
