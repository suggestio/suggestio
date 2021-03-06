package models.req

import io.suggest.n2.node.MNode
import models.adv.{MExtReturn, MExtTarget}
import play.api.data.Form
import play.api.mvc.Request

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.12.15 13:24
 * Description: Реквест сохранения цели внешнего размещения для узла.
 */
trait INodeExtTgSubmitReq[A] extends INodeReq[A] {
  def newTgForm   : Form[(MExtTarget, Option[MExtReturn])]
  def tgExisting  : Option[MExtTarget]
}


case class MNodeExtTgSubmitReq[A](
  override val mnode       : MNode,
  override val newTgForm   : Form[(MExtTarget, Option[MExtReturn])],
  override val tgExisting  : Option[MExtTarget],
  override val request     : Request[A],
  override val user        : ISioUser
)
  extends MReqWrap[A]
  with INodeExtTgSubmitReq[A]
