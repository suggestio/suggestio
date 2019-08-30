package io.suggest.bill.cart.m

import diode.FastEq
import io.suggest.bill.cart.MOrderContent
import io.suggest.jd.MJdData
import io.suggest.jd.render.m.MJdDataJs
import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.order.MOrderStatuses
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.primo.id.OptId
import io.suggest.sc.index.MSc3IndexResp
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.08.2019 15:06
  * Description: Обёртка над MOrderContent, чтобы дополнить данные заказа js-only данными.
  */
object MOrderContentJs {

  implicit object MOrderContentJsFastEq extends FastEq[MOrderContentJs] {
    override def eqv(a: MOrderContentJs, b: MOrderContentJs): Boolean = {
      (a.content ===* b.content)
    }
  }

  implicit def univEq: UnivEq[MOrderContentJs] = UnivEq.derive

}


case class MOrderContentJs(
                            content: MOrderContent,
                          ) {

  /** Сборка инстанса карты ресиверов. Происходит на клиенте, когда наступает необходимость. */
  val adnNodesMap: Map[String, MSc3IndexResp] =
    OptId.els2idMap[String, MSc3IndexResp]( content.adnNodes )

  /** Карта item'ов, сгруппированных по id карточки. */
  val adId2itemsMap: Map[String, Seq[MItem]] =
    content.items.groupBy( _.nodeId )

  /** Рендерить ли чекбокс для управления item'ами? */
  val isItemsEditable: Boolean =
    content.order.isEmpty || content.order.exists( _.status ==* MOrderStatuses.Draft )


  /** Карта данных карточек. */
  val adId2jdDataMap: Map[String, MJdDataJs] = {
    OptId.els2idMap[String, MJdDataJs](
      content.adsJdDatas
        .iterator
        .map( MJdDataJs.apply )
    )
  }

}
