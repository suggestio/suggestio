package io.suggest.jd.tags.qd

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.08.17 14:43
  * Description: Тип операции.
  */

case object MQdOpTypes extends StringEnum[MQdOpType] {

  case object Insert extends MQdOpType("i")

  case object Retain extends MQdOpType("r")

  case object Delete extends MQdOpType("d")


  override val values = findValues

}


/** Класс типа quill-delta операции. */
sealed abstract class MQdOpType(override val value: String) extends StringEnumEntry


object MQdOpType {

  implicit val MQD_OP_TYPE_FORMAT: Format[MQdOpType] = {
    EnumeratumUtil.valueEnumEntryFormat( MQdOpTypes )
  }

  implicit def univEq: UnivEq[MQdOpType] = UnivEq.derive

}
