package models.adv.geo

import models.adv.form.IAdvFormResult
import models.maps.RadMapValue

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.03.16 11:24
  * Description: Общий интерфейс для результатов биндинга форм размещения в географии.
  */
trait IAdvGeoFormResult extends IAdvFormResult {

  /** Данные карты. */
  def radMapVal : RadMapValue

}
