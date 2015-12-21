package models.req

import models.MNode
import models.event.MEvent
import play.api.mvc.Request

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.12.15 10:35
 * Description: Модель реквеста с событием и связанным узлом внутри.
 */
trait INodeEventReq[A] extends INodeReq[A] {
  def mevent: MEvent
}


case class MNodeEventReq[A](
  override val mevent  : MEvent,
  override val mnode   : MNode,
  override val request : Request[A],
  override val user    : ISioUser
)
  extends MReqWrap[A]
  with INodeEventReq[A]
