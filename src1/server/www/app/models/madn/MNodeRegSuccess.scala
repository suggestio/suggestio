package models.madn

import models.MNode
import play.api.mvc.Call

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.15 10:08
 * Description: Модель аргументов для шаблонов regSuccessTpl.
 */
trait INodeRegSuccess {

  /** Созданный узел. */
  def mnode: MNode

  /** Куда юзера редиректить после инсталляции узла. */
  def rdrCall: Call

  /** Сколько секунд ждать при refres-редиректе. */
  def autoRdrAfterSeconds: Int

}


case class MNodeRegSuccess(
  override val mnode                : MNode,
  override val rdrCall              : Call,
  override val autoRdrAfterSeconds  : Int
)
  extends INodeRegSuccess

