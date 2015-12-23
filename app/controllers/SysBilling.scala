package controllers

import models.mproj.ICommonDi
import util.acl.{IsSuNode, IsSuperuser}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.12.15 23:01
 * Description: Контроллер sys-биллинга второго поколения.
 * Второй биллинг имеет тарифы внутри узлов и контракты-ордеры-item'ы в РСУБД.
 */
class SysBilling(
  override val mCommonDi: ICommonDi
)
  extends SioControllerImpl
  with IsSuNode
{

  import mCommonDi._

  def forNode(nodeId: String) = IsSuNode(nodeId).async { implicit request =>
    // Поискать контракт, собрать аргументы для рендера, отрендерить forNodeTpl.
    ???
  }

}
