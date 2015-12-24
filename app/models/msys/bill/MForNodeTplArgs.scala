package models.msys.bill

import models.MNode

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.12.15 14:01
 * Description: Модель аргументов шаблона sys-биллинга одного узла [[views.html.sys1.bill.forNodeTpl]].
 */
trait IForNodeTplArgs {

  def mnode: MNode

}


case class MForNodeTplArgs(
  override val mnode        : MNode
)
  extends IForNodeTplArgs

