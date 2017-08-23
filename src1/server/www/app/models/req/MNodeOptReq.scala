package models.req

import io.suggest.model.n2.node.MNode
import play.api.mvc.Request

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.12.15 23:02
 * Description: Реквест с опциональным узлом внутри.
 * @see [[MNodeReq]]
 */
trait INodeOptReq[A] extends IReq[A] {
  def mnodeOpt: Option[MNode]
}


case class MNodeOptReq[A](
  override val mnodeOpt  : Option[MNode],
  override val request   : Request[A],
  override val user      : ISioUser
)
  extends MReqWrap[A]
  with INodeOptReq[A]
