package io.suggest.init.routed

import enumeratum.values.{StringEnum, StringEnumEntry}
import japgolly.univeq.UnivEq


/** Кросс-платформенная модель целей js-инициализации. */
object MJsInitTargets extends StringEnum[MJsInitTarget] {

  /** Активация всплывающих уведомлений. */
  case object Flashing extends MJsInitTarget("a")

  /** js для формы внешнего размещения карточки. */
  case object LkAdvExtForm extends MJsInitTarget("c")

  /** js-исполнитель внешнего размещения. */
  case object AdvExtRunner extends MJsInitTarget("d")

  /** Цель для инициализации страницы размещения в геотегах. */
  case object AdvGeoForm extends MJsInitTarget("m")

  /** Цель инициализации формы размещения ADN-узла на географической карте. */
  case object AdnMapForm extends MJsInitTarget("o")

  /** Цель инициализации формы управления узлами в ЛК узла. */
  case object LkNodesForm extends MJsInitTarget("p")

  /** Цель инициализации формы ЛК-редактора рекламной карточки на базе react. */
  case object LkAdEditR extends MJsInitTarget("q")

  /** Цель инициализации ЛК-формы управления карточками. */
  case object LkAdsForm extends MJsInitTarget("r")

  /** Цель инициализации react-формы редактирования ADN-узла. */
  case object LkAdnEditForm extends MJsInitTarget("s")

  /** Цель для страницы с корзиной. */
  case object LkCartPageForm extends MJsInitTarget("t")

  /** Цель формы модерации размещений в sys. */
  case object SysMdrForm extends MJsInitTarget("u")

  /** Цель инициализации формы логина. */
  case object LoginForm extends MJsInitTarget("v")

  /** Цель инициализации формы смены пароля. */
  case object PwChange extends MJsInitTarget("w")

  /** Инициализация редактора эджа. */
  case object EdgeEditForm extends MJsInitTarget("x")

  override def values = findValues

}



/** Трейт для всех инстансов модели. */
sealed abstract class MJsInitTarget(override val value: String) extends StringEnumEntry {
  override final def toString = value
}

object MJsInitTarget {
  @inline implicit def univEq: UnivEq[MJsInitTarget] = UnivEq.derive
}
