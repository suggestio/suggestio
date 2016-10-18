package models.req

import io.suggest.model.n2.edge.MEdge
import io.suggest.model.n2.node.MNode
import play.api.mvc.Request

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.10.16 16:28
  * Description: Модель для реквестов, завязанных на какой-то эдж узла.
  */
trait IEdgeReq[A]
  extends IReq[A]
{
  def medge: MEdge
}


/** Гибрид [[IEdgeReq]] и [[INodeReq]]. */
trait INodeEdgeReq[A]
  extends INodeReq[A]
  with IEdgeReq[A]



/** Реализация модели реквеста, завязанного на узел и какой-то его эдж. */
case class MNodeEdgeReq[A](
  override val mnode    : MNode,
  override val medge    : MEdge,
  override val request  : Request[A],
  override val user     : ISioUser
)
  extends MReqWrap[A]
  with INodeEdgeReq[A]
