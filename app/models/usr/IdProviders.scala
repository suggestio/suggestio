package models.usr

import io.suggest.model.EnumMaybeWithName
import securesocial.core.AuthenticationMethod
import securesocial.core.providers.{FacebookProvider, VkProvider, TwitterProvider}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.02.15 18:54
 * Description:
 */


/** Поддерживаемые провайдеры идентификации. */
object IdProviders extends Enumeration with EnumMaybeWithName {

  protected abstract class Val(val strId: String) extends super.Val(strId) {
    def ssAuthMethod: AuthenticationMethod
  }

  override type T = Val

  val Facebook: T   = new Val(FacebookProvider.Facebook) {
    override def ssAuthMethod = AuthenticationMethod.OAuth2
  }

  val Vkontakte: T  = new Val(VkProvider.Vk) {
    override def ssAuthMethod = AuthenticationMethod.OAuth2
  }

  val Twitter: T    = new Val(TwitterProvider.Twitter) {
    override def ssAuthMethod = AuthenticationMethod.OAuth1
  }
}

