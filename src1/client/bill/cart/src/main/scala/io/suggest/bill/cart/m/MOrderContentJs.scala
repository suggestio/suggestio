package io.suggest.bill.cart.m

import diode.FastEq
import io.suggest.bill.cart.MOrderContent
import io.suggest.jd.render.m.MJdDataJs
import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.order.MOrderStatuses
import io.suggest.sc.index.MSc3IndexResp
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.08.2019 15:06
  * Description: MOrderContent wrapper for additional js-side runtime data.
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

  /** Receiver nodes map by node-id. */
  val adnNodesMap: Map[String, MSc3IndexResp] = {
    content.adnNodes
      .zipWithIdIter[String]
      .to( Map )
  }

  /** Ad node id to items data map (Order items grouped by ad id). */
  val adId2itemsMap: Map[String, Seq[MItem]] =
    content.items.groupBy( _.nodeId )

  /** Item selection checkbox can to be rendered? */
  val isItemsEditable: Boolean =
    content.order.isEmpty || content.order.exists( _.status ==* MOrderStatuses.Draft )


  /** Ads jd-render data by ad node id Map. */
  val adId2jdDataMap: Map[String, MJdDataJs] = {
    content.adsJdDatas
      .iterator
      .map( MJdDataJs.fromJdData(_) )
      .zipWithIdIter[String]
      .to( Map )
  }

}
