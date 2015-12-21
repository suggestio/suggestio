package models.req

import models.MNode
import models.adv.MAdvReq
import play.api.mvc.Request

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.12.15 13:37
 * Description: Реквест с инстансом запроса размещения и связанным узлом (ресивером, изначально).
 */
trait IAdvReqReq[A] extends IReq[A] {
  def advReq: MAdvReq
}

trait INodeAdvReqReq[A]
  extends INodeReq[A]
  with IAdvReqReq[A]


case class MNodeAdvReqReq[A](
  override val advReq  : MAdvReq,
  override val mnode   : MNode,
  override val request : Request[A],
  override val user    : ISioUser
)
  extends MReqWrap[A]
  with INodeAdvReqReq[A]
