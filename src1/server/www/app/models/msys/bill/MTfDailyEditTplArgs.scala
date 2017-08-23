package models.msys.bill

import io.suggest.model.n2.bill.tariff.daily.MTfDaily
import io.suggest.model.n2.node.MNode
import models.mcal.MCalendar
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
  def tf: Form[MTfDaily]

}

trait ITfDailyEditTplArgs extends ITfDailyFormTplArgs {

  /** Текущая редактируемая нода. */
  def mnode: MNode

}


/** Дефолтовая реализация модели [[ITfDailyEditTplArgs]]. */
case class MTfDailyEditTplArgs(
  override val mnode        : MNode,
  override val mcals        : Seq[MCalendar],
  override val tf           : Form[MTfDaily]
)
  extends ITfDailyEditTplArgs
