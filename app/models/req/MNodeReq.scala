package models.req

import models.MNode
import play.api.mvc.Request

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.12.15 18:17
 * Description: Модель реквеста с каким-то узлом внутри.
 */
trait INodeReq[A]
  extends ISioReq[A]
{
  def mnode: MNode
}


/** Реализация модели реквеста с узлом внутри. */
case class MNodeReq[A](
  override val mnode    : MNode,
  override val request  : Request[A],
  override val user     : ISioUser
)
  extends SioReqWrap[A]
  with INodeReq[A]

