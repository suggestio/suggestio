package models.req

import io.suggest.model.n2.node.MNode
import play.api.mvc.Request

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.12.15 22:09
 * Description: Реквест с узлом юзера внутри.
 */

trait IPersonReq[A] extends IReq[A] {
  def mperson   : MNode
}


case class MPersonReq[A](
  override val mperson : MNode,
  override val request : Request[A],
  override val user    : ISioUser
)
  extends MReqWrap[A]
  with IPersonReq[A]
