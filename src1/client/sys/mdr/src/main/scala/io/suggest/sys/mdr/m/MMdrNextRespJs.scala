package io.suggest.sys.mdr.m

import io.suggest.jd.render.m.MJdDataJs
import io.suggest.primo.id.OptId
import io.suggest.sc.index.MSc3IndexResp
import io.suggest.sys.mdr.{MMdrNextResp, MNodeMdrInfo}
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.08.2019 16:01
  * Description: js-обёртки над моделями mdr-resp.
  */
object MMdrNextRespJs {
  @inline implicit def univEq: UnivEq[MMdrNextRespJs] = UnivEq.derive
  val resp = GenLens[MMdrNextRespJs](_.resp)
}
case class MMdrNextRespJs(
                           resp: MMdrNextResp,
                         ) {

  val nodeOpt = resp.nodeOpt
    .map( MNodeMdrInfoJs.apply )

}


object MNodeMdrInfoJs {
  @inline implicit def univEq: UnivEq[MNodeMdrInfoJs] = UnivEq.derive
  val info = GenLens[MNodeMdrInfoJs](_.info)
}
case class MNodeMdrInfoJs(
                           info: MNodeMdrInfo
                         ) {

  val ad = info.ad.map(MJdDataJs.fromJdData(_))

  /** Сгруппированные item'ы по типам. */
  val itemsByType = info.items.groupBy(_.iType)

  /** Карта узлов по id. */
  val nodesMap = OptId.els2idMap[String, MSc3IndexResp]( info.nodes )

  /** Список бесплатных размещений на "своих" узлах в обход биллингов. */
  val directSelfNodesSorted: Seq[MSc3IndexResp] = {
    info.directSelfNodeIds
      .iterator
      .flatMap( nodesMap.get )
      .toSeq
      .sortBy( _.nameOrIdOrEmpty )
  }

  val mdrNodeOpt: Option[MSc3IndexResp] =
    nodesMap.get( info.nodeId )

}
