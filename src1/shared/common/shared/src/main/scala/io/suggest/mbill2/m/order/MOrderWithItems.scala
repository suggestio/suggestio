package io.suggest.mbill2.m.order

import io.suggest.mbill2.m.item.MItem
import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.02.16 13:17
  * Description: Container for billing order with items inside.
  */
case class MOrderWithItems(
                            order: MOrder,
                            items: Seq[MItem],
                          )

object MOrderWithItems {

  @inline implicit def univEq: UnivEq[MOrderWithItems] = UnivEq.derive


  implicit def orderWithItemsJson: OFormat[MOrderWithItems] = {
    (
      (__ \ "order").format[MOrder] and
      (__ \ "items").format[Seq[MItem]]
    )(apply, unlift(unapply))
  }

}
