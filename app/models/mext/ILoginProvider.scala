package models.mext

import play.api.mvc.PathBindable
import securesocial.core.AuthenticationMethod
import securesocial.core.providers.ProviderCompanion
import scala.language.implicitConversions

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.04.15 18:27
 * Description: Поддержка входа на suggest.io через текущий сервис и SecureSocial.
 * Модель появилась из models.usr.IdProviders, которая была замержена в MExtServices 2015.apr.15 после a6c8a619bdff.
 */
object ILoginProvider {

  /** Обратиться к модели [[MExtServices]] и узнать там провайдера по имени. */
  def maybeWithName(n: String): Option[ILoginProvider] = {
    MExtServices.values
      .iterator
      .flatMap { _.loginProvider }
      .find { _.ssProvName == n }
  }


  /** Поддержка маппинга провайдера из URL path. */
  implicit def pb(implicit strB: PathBindable[String]): PathBindable[ILoginProvider] = {
    new PathBindable[ILoginProvider] {
      override def bind(key: String, value: String): Either[String, ILoginProvider] = {
        strB.bind(key, value).right.flatMap { provId =>
          maybeWithName(provId) match {
            case Some(prov) => Right(prov)
            case None       => Left("e.id.unknown.provider")
          }
        }
      }

      override def unbind(key: String, value: ILoginProvider): String = {
        strB.unbind(key, value.ssProvName)
      }
    }
  }

}


/** Интерфейс для sign-in. */
trait ILoginProvider {

  /** Метод аутентификации SecureSocial. */
  def ssAuthMethod  : AuthenticationMethod

  /** SecureSocial provider companion. */
  def ssProvider    : ProviderCompanion

  /** Имя провайдера по мнению SecureSocial. */
  def ssProvName    : String

}
