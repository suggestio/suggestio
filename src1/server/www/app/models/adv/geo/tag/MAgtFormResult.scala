package models.adv.geo.tag

import models.adv.form.MDatesPeriod
import models.adv.geo.IAdvGeoFormResult
import models.adv.geo.mapf.MRcvrBindedInfo
import models.maps.RadMapValue
import models.mtag.MTagBinded

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.11.15 22:35
 * Description: Результат маппинга формы размещения.
 */
@deprecated("Use MFormS/MFormInit instead", "2016.jan.11")
trait IAgtFormResult
  extends IAdvGeoFormResult
{

  /** Также разместить вне тегов на главном экране выдачи в указанном месте. */
  def onMainScreen: Boolean

  /** 2016.dec.13: Инфа по прямому размещению доступна через эту форму. */
  def rcvrs: Seq[MRcvrBindedInfo]

  /** Теги, заданные юзером. */
  def tags      : List[MTagBinded]

}


@deprecated("Use MFormS/MFormInit instead", "2016.jan.11")
case class MAgtFormResult(
  override val tags         : List[MTagBinded],
  override val radMapVal    : RadMapValue,
  override val period       : MDatesPeriod,
  override val onMainScreen : Boolean,
  override val rcvrs        : List[MRcvrBindedInfo]
)
  extends IAgtFormResult
