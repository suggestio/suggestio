package io.suggest.bill.cart

import diode.FastEq
import io.suggest.common.empty.EmptyUtil
import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.order.MOrder
import io.suggest.mbill2.m.txn.MTxn
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.09.18 16:26
  * Description: JSON-модель с данными ордера, корзины в частности.
  */
object MOrderContent {

  /** Поддержка FastEq для инстансов [[MOrderContent]]. */
  implicit object MOrderContentFastEq extends FastEq[MOrderContent] {
    override def eqv(a: MOrderContent, b: MOrderContent): Boolean = {
      (a.order ===* b.order) &&
      (a.items ===* b.items) &&
      (a.txns  ===* b.txns)
    }
  }

  /** Поддержка play-json. */
  implicit def mCartRootSFormat: OFormat[MOrderContent] = {
    (
      (__ \ "o").formatNullable[MOrder] and
      (__ \ "i").formatNullable[Seq[MItem]]
        .inmap[Seq[MItem]](
          EmptyUtil.opt2ImplEmpty1F(Nil),
          { items => if(items.isEmpty) None else Some(items) }
        ) and
      (__ \ "t").formatNullable[Seq[MTxn]]
        .inmap[Seq[MTxn]](
          EmptyUtil.opt2ImplEmpty1F(Nil),
          { txns => if(txns.isEmpty) None else Some(txns) }
        )
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MOrderContent] = UnivEq.derive

}


/** Класс-контейнер данных одного ордера, если он есть.
  *
  * @param order Данные ордера.
  * @param items Содержимое ордера.
  * @param txns Денежные транзакции по ордеру.
  */
case class MOrderContent(
                          order    : Option[MOrder],
                          items    : Seq[MItem],
                          txns     : Seq[MTxn]
                        )
