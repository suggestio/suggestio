package models.req

import models.MNode
import play.api.mvc.Request

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.12.15 22:09
 * Description: Реквест с узлом юзера внутри.
 */

trait IPersonReq[A] extends ISioReq[A] {
  def mperson   : MNode
}


case class MPersonReq[A](
  override val mperson : MNode,
  override val request : Request[A],
  override val user    : ISioUser
)
  extends SioReqWrap[A]
  with IPersonReq[A]
