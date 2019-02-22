package models.mext

import io.suggest.ext.svc.MExtServices
import io.suggest.model.play.psb.PathBindableImpl
import play.api.libs.json._
import play.api.mvc.PathBindable
import securesocial.core.providers.ProviderCompanion

import scala.language.implicitConversions
import scala.reflect.ClassTag

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.04.15 18:27
 * Description: Поддержка входа на suggest.io через текущий сервис и SecureSocial.
 * Модель появилась из models.usr.IdProviders, которая была замержена в MExtServices 2015.apr.15 после a6c8a619bdff.
 */
object ILoginProvider {

  def valuesIter: Iterator[ILoginProvider] = {
    for {
      svc <- MExtServices.values.iterator
      svcJvm = MExtServicesJvm.forService( svc )
      idp <- svcJvm.loginProvider
    } yield idp
  }

  /** Обратиться к модели [[MExtServices]] и узнать там провайдера по имени. */
  def maybeWithName(n: String): Option[ILoginProvider] = {
    valuesIter
      .find { _.ssProvName == n }
  }


  /** Поддержка маппинга провайдера из URL path. */
  implicit def pb(implicit strB: PathBindable[String]): PathBindable[ILoginProvider] = {
    new PathBindableImpl[ILoginProvider] {
      override def bind(key: String, value: String): Either[String, ILoginProvider] = {
        strB.bind(key, value).right.flatMap { provId =>
          maybeWithName(provId)
            .toRight( "e.id.unknown.provider" )
        }
      }

      override def unbind(key: String, value: ILoginProvider): String = {
        strB.unbind(key, value.ssProvName)
      }
    }
  }

  /** JSON deserializer. */
  implicit def reads: Reads[ILoginProvider] = {
    implicitly[Reads[String]]
      // TODO заменить это на какой-нибудь flatMap().
      .map { maybeWithName }
      .filter { _.nonEmpty }
      .map { _.get }
  }

}


/** Интерфейс для sign-in. */
trait ILoginProvider {

  /** SecureSocial provider companion. */
  def ssProviderClass: ClassTag[_ <: ProviderCompanion]

  /** Имя провайдера по мнению SecureSocial. */
  def ssProvName    : String

}
