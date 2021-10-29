package io.suggest.bill.cart

import diode.FastEq
import io.suggest.bill.MPrice
import io.suggest.common.empty.EmptyUtil
import io.suggest.jd.MJdData
import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.order.MOrder
import io.suggest.mbill2.m.txn.MTxnPriced
import io.suggest.sc.index.MSc3IndexResp
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import monocle.macros.GenLens
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
      (a.txns  ===* b.txns) &&
      (a.adnNodes ===* b.adnNodes) &&
      (a.adsJdDatas ===* b.adsJdDatas) &&
      (a.orderPrices ===* b.orderPrices)
    }
  }

  /** Поддержка play-json. */
  implicit def mCartRootSFormat: OFormat[MOrderContent] = {
    (
      (__ \ "o").formatNullable[MOrder] and
      (__ \ "i").formatNullable[Seq[MItem]]
        .inmap[Seq[MItem]](
          EmptyUtil.opt2ImplEmpty1F(Nil),
          { items => if (items.isEmpty) None else Some(items) }
        ) and
      (__ \ "t").formatNullable[Seq[MTxnPriced]]
        .inmap[Seq[MTxnPriced]](
          EmptyUtil.opt2ImplEmpty1F(Nil),
          { txns => if (txns.isEmpty) None else Some(txns) }
        ) and
      (__ \ "r").formatNullable[Iterable[MSc3IndexResp]]
        .inmap[Iterable[MSc3IndexResp]](
          EmptyUtil.opt2ImplEmpty1F( Nil ),
          { rcvrs => if (rcvrs.isEmpty) None else Some(rcvrs) }
        ) and
      (__ \ "j").formatNullable[Iterable[MJdData]]
        .inmap[Iterable[MJdData]](
          EmptyUtil.opt2ImplEmpty1F( Nil ),
          { jds => if (jds.isEmpty) None else Some(jds) }
        ) and
      (__ \ "p").formatNullable[Iterable[MPrice]]
        .inmap[Iterable[MPrice]](
          EmptyUtil.opt2ImplEmpty1F( Nil ),
          { prices => if (prices.isEmpty) None else Some(prices) }
        )
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MOrderContent] = UnivEq.derive

  def order = GenLens[MOrderContent](_.order)

}


/** Класс-контейнер данных одного ордера, если он есть.
  *
  * @param order Данные ордера.
  * @param items Содержимое ордера.
  * @param txns Денежные транзакции по ордеру.
  * @param orderPrices Стоимость заказа.
  * @param adnNodes ADN-узлы из item.nodeId, item.rcvrId и проч.
  */
case class MOrderContent(
                          order       : Option[MOrder],
                          items       : Seq[MItem]                = Nil,
                          txns        : Seq[MTxnPriced]           = Nil,
                          adnNodes    : Iterable[MSc3IndexResp]   = Nil,
                          adsJdDatas  : Iterable[MJdData]         = Nil,
                          orderPrices : Iterable[MPrice]          = Nil,
                        )
