package models.adv.ext

import models.MNode
import models.adv._
import play.api.data.Form

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.10.15 11:27
 * Description: Модель контейнера аргументов шаблона adv.ext.forAdTpl.
 */
trait IForAdTplArgs extends IAdvFreeForAdCommonTplArgs {

  /** Список текущих целей для внешнего размещения. */
  def targets     : Seq[MExtTarget]

  /** Маппинг формы внешнего размещения в целом. */
  def advForm     : Form[List[MExtTargetInfo]]

  /** Функция сборки маппинга формы для редактирования одной указанной цели. */
  def oneTgForm   : MExtTarget => OneExtTgForm

}


case class MForAdTplArgs(
  override val mad        : MNode,
  override val producer   : MNode,
  override val targets    : Seq[MExtTarget],
  override val advForm    : Form[List[MExtTargetInfo]],
  override val oneTgForm  : MExtTarget => OneExtTgForm
)
  extends IForAdTplArgs
