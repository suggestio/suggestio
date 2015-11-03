package models.mbill

import models.im.MImgT
import models.MNode

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.10.15 15:46
 * Description: Модель аргументов для шаблонов с инфой о daily-тарификации размещения узла.
 */

trait IDailyMmpsTplArgs {

  /** daily-тарифы. */
  def tariffs   : List[MTariffDaily]

  /** Узел. */
  def mnode     : MNode

  /** Галлерея узла. */
  def gallery   : Seq[MImgT]

}


case class MDailyMmpsTplArgs(
  override val tariffs   : List[MTariffDaily],
  override val mnode   : MNode,
  override val gallery   : Seq[MImgT]
)
  extends IDailyMmpsTplArgs
