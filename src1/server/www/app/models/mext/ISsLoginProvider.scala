package models.mext

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

/** Интерфейс для sign-in. */
trait ISsLoginProvider {

  /** SecureSocial provider companion. */
  def ssProviderClass: ClassTag[_ <: ProviderCompanion]

  /** Имя провайдера по мнению SecureSocial. */
  def ssProvName    : String

}

// TODO Надо удалить/заинлайнить этот интерфейс.
