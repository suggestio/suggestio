package models.adv.geo.tag

import models.adv.form.MDatesPeriod
import models.maps.RadMapValue
import models.mtag.MTagBinded

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.11.15 22:35
 * Description: Результат маппинга формы размещения.
 */
trait IAgtFormResult {

  /** Теги, заданные юзером. */
  def tags      : List[MTagBinded]

  /** Период размещения. */
  def dates     : MDatesPeriod

  /** Данные карты. */
  def radMapVal : RadMapValue

}


case class MAgtFormResult(
  override val tags       : List[MTagBinded],
  override val radMapVal  : RadMapValue,
  override val dates      : MDatesPeriod
)
  extends IAgtFormResult
