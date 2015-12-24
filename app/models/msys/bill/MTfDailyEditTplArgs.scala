package models.msys.bill

import models.{MCalendar, MDailyTf, MNode}
import play.api.data.Form

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.12.15 18:32
 * Description: Аргументы для шаблона страницы редактирования посуточного тарифа.
 */

trait ITfDailyFormTplArgs {

  /** Доступные календари. */
  def mcals: Seq[MCalendar]

  /** Маппинг формы. */
  def tf: Form[MDailyTf]

}

trait ITfDailyEditTplArgs extends ITfDailyFormTplArgs {

  /** Текущая редактируемая нода. */
  def mnode: MNode

}


/** Дефолтовая реализация модели [[ITfDailyEditTplArgs]]. */
case class MTfDailyEditTplArgs(
  override val mnode        : MNode,
  override val mcals        : Seq[MCalendar],
  override val tf           : Form[MDailyTf]
)
  extends ITfDailyEditTplArgs
