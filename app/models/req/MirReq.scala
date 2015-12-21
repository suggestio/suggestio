package models.req

import models.MInviteRequest
import play.api.mvc.Request

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.12.15 21:53
 * Description: Реквест с экземпляром MInviteRequest внутри.
 */
trait IMirReq[A] extends IReq[A] {
  def mir: MInviteRequest
}


case class MirReq[A](
  override val mir       : MInviteRequest,
  override val request   : Request[A],
  override val user      : ISioUser
)
  extends MReqWrap[A]
  with IMirReq[A]

