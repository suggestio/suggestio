package io.suggest.pay.yookassa

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.ueq.UnivEqUtil._


case class MYkObject(
                      typ   : MYkObjectType,
                      event : Option[MYkEventType] = None,
                      obj   : JsObject,
                    )

object MYkObject {

  @inline implicit def univEq: UnivEq[MYkObject] = UnivEq.derive

  implicit def ykObjectJson: OFormat[MYkObject] = {
    (
      (__ \ "type").format[MYkObjectType] and
      (__ \ "event").formatNullable[MYkEventType] and
      (__ \ "object").format[JsObject]
    )(apply, unlift(unapply))
  }

}