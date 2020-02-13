package models.req

import io.suggest.n2.edge.MEdge
import io.suggest.n2.media.MEdgeMedia
import io.suggest.n2.node.MNode
import play.api.mvc.Request

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.12.15 18:17
 * Description: Модель реквеста с каким-то узлом внутри.
 */
trait INodeReq[A] extends IReq[A] {
  def mnode: MNode
}


trait IEdgeReq[A] extends INodeReq[A] {
  def edge: MEdge
}


/** Реквест узла с доступом к эджу. */
trait IEdgeMediaReq[A] extends IEdgeReq[A] {
  def edgeMedia: MEdgeMedia
}


/** Реализация модели реквеста с узлом внутри. */
case class MNodeReq[A](
  override val mnode    : MNode,
  override val request  : Request[A],
  override val user     : ISioUser
)
  extends MReqWrap[A]
  with INodeReq[A]
