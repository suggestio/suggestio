package models.adv.gtag

import io.suggest.model.geo.CircleGs
import models.adv.form.MDatesPeriod
import models.maps.MapViewState
import models.mtag.MTagBinded
import org.joda.time.Interval

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.11.15 22:35
 * Description: Результат маппинга формы размещения.
 */
case class MAdvFormResult(
  tags      : List[MTagBinded],
  mapState  : MapViewState,
  circle    : CircleGs,
  period    : MDatesPeriod
) {

  def interval = new Interval(period.dateStart.toDateTimeAtStartOfDay, period.dateEnd.toDateTimeAtStartOfDay)

}
