package models.usr

import io.suggest.model.EnumMaybeWithName
import play.api.mvc.{PathBindable, QueryStringBindable}
import securesocial.core.AuthenticationMethod
import securesocial.core.providers.{FacebookProvider, VkProvider, TwitterProvider}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.02.15 18:54
 * Description: Модель используемых в s.io провайдеров идентификации.
 * Порядок экземпляров здесь определяет порядок оных на странице.
 */

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


  implicit def pb(implicit strB: PathBindable[String]) = new PathBindable[IdProvider] {
    override def bind(key: String, value: String): Either[String, IdProvider] = {
      strB.bind(key, value).right.flatMap { provId =>
        maybeWithName(provId) match {
          case Some(prov) => Right(prov)
          case None       => Left("e.id.unknown.provider")
        }
      }
    }

    override def unbind(key: String, value: IdProvider): String = {
      strB.unbind(key, value.strId)
    }
  }

}

