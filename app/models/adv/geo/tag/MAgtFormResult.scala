package models.adv.geo.tag

import models.adv.form.MDatesPeriod
import models.adv.geo.IAdvGeoFormResult
import models.maps.RadMapValue
import models.mtag.MTagBinded

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.11.15 22:35
 * Description: Результат маппинга формы размещения.
 */
trait IAgtFormResult
  extends IAdvGeoFormResult
  with IFormTags
{

  /** Также разместить вне тегов на главном экране выдачи в указанном месте. */
  def onMainScreen: Boolean

}


/** Интерфейс для поля тегов. */
trait IFormTags {

  /** Теги, заданные юзером. */
  def tags      : List[MTagBinded]

}


case class MAgtFormResult(
  override val tags         : List[MTagBinded],
  override val radMapVal    : RadMapValue,
  override val period       : MDatesPeriod,
  override val onMainScreen : Boolean
)
  extends IAgtFormResult
