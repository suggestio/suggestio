package io.suggest.pay.yookassa

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format


object MYkObjectTypes extends StringEnum[MYkObjectType] {

  case object Notification extends MYkObjectType("notification")

  override def values = findValues

}


sealed abstract class MYkObjectType(override val value: String ) extends StringEnumEntry

object MYkObjectType {

  @inline implicit def univEq: UnivEq[MYkObjectType] = UnivEq.derive

  implicit def ykObjectTypeJson: Format[MYkObjectType] =
    EnumeratumUtil.valueEnumEntryFormat( MYkObjectTypes )

}
