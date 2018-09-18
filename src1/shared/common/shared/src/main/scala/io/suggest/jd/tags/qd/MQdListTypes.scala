package io.suggest.jd.tags.qd

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.09.17 21:40
  * Description: Модель типов списков.
  */

object MQdListTypes extends StringEnum[MQdListType] {

  /** Тип списка: нумерованный список. */
  case object Ordered extends MQdListType( "ordered" )

  /** Тип списка: маркерованный список. */
  case object Bullet extends MQdListType( "bullet" )

  override val values = findValues

}


/** Класс одного элемента модели типов списка. */
sealed abstract class MQdListType(override val value: String) extends StringEnumEntry {
  override final def toString = value
}


object MQdListType {

  /** Поддержка UnivEq. */
  @inline implicit def univEq: UnivEq[MQdListType] = UnivEq.derive

  /** Поддержка play-json. */
  implicit val MQD_LIST_TYPE_FORMAT: Format[MQdListType] = {
    EnumeratumUtil.valueEnumEntryFormat( MQdListTypes )
  }

}
