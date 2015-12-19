package models.req

import models.MNode
import models.adv.MExtTarget
import play.api.mvc.Request

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.12.15 11:46
 * Description: Модель реквеста с целью внешнего размещения + узлел-владелец цели.
 */
trait IExtTargetNodeReq[A] extends INodeReq[A] {
  def extTarget: MExtTarget
}


case class MExtTargetNodeReq[A](
  override val extTarget : MExtTarget,
  override val mnode     : MNode,
  override val request   : Request[A],
  override val user      : ISioUser
)
  extends ISioReqWrap[A]
  with IExtTargetNodeReq[A]
