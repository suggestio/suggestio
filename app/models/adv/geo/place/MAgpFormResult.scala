package models.adv.geo.place

import models.adv.form.MDatesPeriod
import models.adv.geo.IAdvGeoFormResult
import models.maps.RadMapValue

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.03.16 13:38
  * Description: Модель результата биндинга формы размещения в произвольном месте на карте.
  */
trait IAgpFormResult
  extends IAdvGeoFormResult


case class MAgpFormResult(
  override val radMapVal  : RadMapValue,
  override val period     : MDatesPeriod
)
  extends IAgpFormResult
