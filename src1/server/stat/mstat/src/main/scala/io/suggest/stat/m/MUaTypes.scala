package io.suggest.stat.m

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.11.16 18:22
  * Description: Модель типов юзер-агента.
  * Изначально "браузер" или "приложение".
  */

object MUaTypes extends StringEnum[MUaType] {

  /** Браузеры. */
  case object Browser extends MUaType("browser")

  /** Мобильные приложения. */
  case object App extends MUaType("app")

  /** Приложение на базе cordova. Используется в связке с MobileApp. */
  case object CordovaApp extends MUaType("cordova")


  override def values = findValues

}


sealed abstract class MUaType(override val value: String) extends StringEnumEntry

object MUaType {

  implicit def mUaTypeFormat: Format[MUaType] =
    EnumeratumUtil.valueEnumEntryFormat( MUaTypes )

  implicit def univEq: UnivEq[MUaType] = UnivEq.derive

}
