package models.req

import models.MNode
import models.usr.{EmailPwIdent, EmailActivation}
import play.api.mvc.Request

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.12.15 22:29
 * Description: Реквест с данными активации узла (реализация инвайта на узел).
 */
trait INodeEactReq[A] extends INodeReq[A] {
  def eact      : EmailActivation
  def epwIdOpt  : Option[EmailPwIdent]
}


case class MNodeEactReq[A](
  override val mnode     : MNode,
  override val eact      : EmailActivation,
  override val epwIdOpt  : Option[EmailPwIdent],
  override val request   : Request[A],
  override val user      : ISioUser
)
  extends SioReqWrap[A]
  with INodeEactReq[A]
