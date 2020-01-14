package models.req

import io.suggest.n2.edge.MEdge
import io.suggest.n2.node.MNode
import play.api.mvc.Request

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.10.16 16:28
  * Description: Модель для реквестов, завязанных на какой-то эдж узла.
  */
trait IEdgeOptReq[A]
  extends IReq[A]
{
  def edgeOpt: Option[MEdge]
}


/** Гибрид [[IEdgeOptReq]] и [[INodeReq]]. */
trait INodeEdgeOptReq[A]
  extends INodeReq[A]
  with IEdgeOptReq[A]



/** Реализация модели реквеста, завязанного на узел и какой-то его эдж. */
case class MNodeEdgeOptReq[A](
                               override val mnode    : MNode,
                               override val edgeOpt  : Option[MEdge],
                               override val request  : Request[A],
                               override val user     : ISioUser
)
  extends MReqWrap[A]
  with INodeEdgeOptReq[A]
