package io.suggest.bill.cart

import diode.FastEq
import io.suggest.bill.MPrice
import io.suggest.common.empty.EmptyUtil
import io.suggest.jd.MJdAdData
import io.suggest.maps.nodes.MAdvGeoMapNodeProps
import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.order.{MOrder, MOrderStatuses}
import io.suggest.mbill2.m.txn.MTxnPriced
import io.suggest.primo.id.{IId, OptId}
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
      (a.txns  ===* b.txns) &&
      (a.adnNodes ===* b.adnNodes)
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
      (__ \ "r").formatNullable[Iterable[MAdvGeoMapNodeProps]]
        .inmap[Iterable[MAdvGeoMapNodeProps]](
          EmptyUtil.opt2ImplEmpty1F( Nil ),
          { rcvrs => if (rcvrs.isEmpty) None else Some(rcvrs) }
        ) and
      (__ \ "j").formatNullable[Iterable[MJdAdData]]
        .inmap[Iterable[MJdAdData]](
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

}


/** Класс-контейнер данных одного ордера, если он есть.
  *
  * @param order Данные ордера.
  * @param items Содержимое ордера.
  * @param txns Денежные транзакции по ордеру.
  * @param orderPrice Полная стоимость заказа.
  * @param adnNodes ADN-узлы из item.nodeId, item.rcvrId и проч.
  */
case class MOrderContent(
                          order       : Option[MOrder],
                          items       : Seq[MItem],
                          txns        : Seq[MTxnPriced],
                          adnNodes    : Iterable[MAdvGeoMapNodeProps],
                          adsJdDatas  : Iterable[MJdAdData],
                          orderPrices : Iterable[MPrice]
                        ) {

  /** Сборка инстанса карты ресиверов. Происходит на клиенте, когда наступает необходимость. */
  lazy val adnNodesMap: Map[String, MAdvGeoMapNodeProps] =
    IId.els2idMap[String, MAdvGeoMapNodeProps]( adnNodes )

  /** Сборка карты отрендеренных карточек. */
  lazy val adId2jdDataMap: Map[String, MJdAdData] =
    OptId.els2idMap[String, MJdAdData]( adsJdDatas )

  /** Карта item'ов, сгруппированных по id карточки. */
  lazy val adId2itemsMap: Map[String, Seq[MItem]] =
    items.groupBy( _.nodeId )


  /** Рендерить ли чекбокс для управления item'ами? */
  lazy val isItemsEditable: Boolean =
    order.isEmpty || order.exists( _.status ==* MOrderStatuses.Draft )

}
