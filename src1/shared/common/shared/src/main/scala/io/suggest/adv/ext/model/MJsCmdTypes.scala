package io.suggest.adv.ext.model

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.02.15 16:01
 * Description: Стройматериалы для реализации модели типов js-комманд от sio-сервера.
 */

/** Типы команд. */
object MJsCmdTypes extends StringEnum[MJsCmdType] {

  /** Команда содержит js-код, который надо тупо исполнить. */
  case object JavaScript extends MJsCmdType( "js" )

  /** Команда содержит json-описание действия, которое должен понять и исполнить js-модуль ext.adv. */
  case object Action extends MJsCmdType("action")


  override def values = findValues

}


sealed abstract class MJsCmdType(override val value: String) extends StringEnumEntry {
  override final def toString = value
}

object MJsCmdType {

  implicit def mJsCmdTypeFormat: Format[MJsCmdType] =
    EnumeratumUtil.valueEnumEntryFormat( MJsCmdTypes )

  @inline implicit def univEq: UnivEq[MJsCmdType] = UnivEq.derive

}
