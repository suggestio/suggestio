package io.suggest.init.routed

import enumeratum.values.{StringEnum, StringEnumEntry}
import japgolly.univeq.UnivEq


/** Кросс-платформенная модель целей js-инициализации. */
object MJsiTgs extends StringEnum[MJsiTg] {

  /** Активация всплывающих уведомлений. */
  case object Flashing extends MJsiTg("a")

  /** js для формы внешнего размещения карточки. */
  case object LkAdvExtForm extends MJsiTg("c")

  /** js-исполнитель внешнего размещения. */
  case object AdvExtRunner extends MJsiTg("d")

  /** Вертикальная центровка в ident. */
  case object IdentVCenterContent extends MJsiTg("e")

  /** init-target для включения js к форме ввода капчи. */
  case object CaptchaForm extends MJsiTg("j")

  /** Цель инициализации для поддержки скрытой капчи, загружаемой и отображаемой опционально. */
  case object HiddenCaptcha extends MJsiTg("k")

  /** Цель для инициализации страницы списка транзакций биллинга. */
  case object BillTxnsList extends MJsiTg("l")

  /** Цель для инициализации страницы размещения в геотегах. */
  case object AdvGeoForm extends MJsiTg("m")

  /** Цель инициализации формы размещения ADN-узла на географической карте. */
  case object AdnMapForm extends MJsiTg("o")

  /** Цель инициализации формы управления узлами в ЛК узла. */
  case object LkNodesForm extends MJsiTg("p")

  /** Цель инициализации формы ЛК-редактора рекламной карточки на базе react. */
  case object LkAdEditR extends MJsiTg("q")

  /** Цель инициализации ЛК-формы управления карточками. */
  case object LkAdsForm extends MJsiTg("r")

  /** Цель инициализации react-формы редактирования ADN-узла. */
  case object LkAdnEditForm extends MJsiTg("s")


  override def values = findValues

}



/** Трейт для всех инстансов модели. */
sealed abstract class MJsiTg(override val value: String) extends StringEnumEntry {
  override final def toString = value
}

object MJsiTg {
  implicit def univEq: UnivEq[MJsiTg] = UnivEq.derive
}
