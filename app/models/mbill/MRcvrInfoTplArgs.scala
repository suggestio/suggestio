package models.mbill

import models.im.MImgT
import models.{MDailyTf, MNode}
import models.mcal.MCalendar

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.10.15 15:46
 * Description: Модель аргументов для шаблонов с инфой о daily-тарификации размещения узла.
 *
 * @see [[views.html.lk.billing._rcvrInfoTpl]]
 */

trait IRcvrInfoTplArgs extends IDailyTfTplArgs {

  /** Галлерея узла. */
  def gallery   : Seq[MImgT]

}


case class MRcvrInfoTplArgs(
  override val mnode      : MNode,
  override val dailyTf    : MDailyTf,
  override val calsMap    : Map[String, MCalendar],
  override val gallery    : Seq[MImgT]
)
  extends IRcvrInfoTplArgs
