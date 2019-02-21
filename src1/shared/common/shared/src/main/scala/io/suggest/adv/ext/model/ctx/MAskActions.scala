package io.suggest.adv.ext.model.ctx

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format


/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.02.15 11:44
 * Description: Модель допустимых запрашиваемых действий по отношению js ext.adv подсистеме со стороны сервера sio.
 */
object MAskActions extends StringEnum[MAskAction] {

  /** Базовая инициализация системы без конкретных адаптеров. */
  case object Init extends MAskAction("c")

  /** Запрос подготовки к работе в рамках доменов. */
  case object EnsureReady extends MAskAction("a")

  /** Запрос публикации одной цели. */
  case object HandleTarget extends MAskAction("b")

  /** Запрос на чтение хранилища. */
  case object StorageGet extends MAskAction("d")

  /** Запрос на запись/стирание значение из хранилища. */
  case object StorageSet extends MAskAction("e")


  override def values = findValues

}


sealed abstract class MAskAction(override val value: String) extends StringEnumEntry

object MAskAction {

  implicit def mAskActionFormat: Format[MAskAction] =
    EnumeratumUtil.valueEnumEntryFormat( MAskActions )

  @inline implicit def univEq: UnivEq[MAskAction] = UnivEq.derive

}

