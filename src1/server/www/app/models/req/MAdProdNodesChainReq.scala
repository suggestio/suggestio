package models.req

import io.suggest.model.n2.node.MNode
import play.api.mvc.Request

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.03.17 16:17
  * Description: Модель реквеста с данными по рекламной карточке и цепочке узлов.
  */
trait IAdProdNodesChainReq[A]
  extends IAdProdReq[A]
  with INodesChainReq[A]


/** Дефолтовая реализация [[IAdProdNodesChainReq]]. */
case class MAdProdNodesChainReq[A](
                                    override val mad          : MNode,
                                    override val producer     : MNode,
                                    override val nodesChain   : Seq[MNode],
                                    override val request      : Request[A],
                                    override val user         : ISioUser
                                  )
  extends MReqWrap[A]
  with IAdProdNodesChainReq[A]
