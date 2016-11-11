package io.suggest.stat.m

import io.suggest.common.menum.{EnumMaybeWithName, StrIdValT}
import io.suggest.model.menum.EnumJsonReadsValT

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.11.16 18:22
  * Description: Модель типов юзер-агента.
  * Изначально "браузер" или "приложение".
  */
object MUaTypes extends EnumMaybeWithName with EnumJsonReadsValT with StrIdValT {

  protected class Val(override val strId: String)
    extends super.Val(strId)
    with ValT

  override type T = Val

  /** Браузеры. */
  val Browser       : T = new Val("browser")

  /** Мобильные приложения. */
  val MobileApp     : T = new Val("mob.app")

  /** Приложение на базе cordova. Используется в связке с MobileApp. */
  val CordovaApp    : T = new Val("cordova")

}
