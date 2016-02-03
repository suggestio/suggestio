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
trait IAdvGeoTagsInfo {

  /** Теги, заданные юзером. */
  def tags      : List[MTagBinded]

  /** Гео-круг размещения, заданный юзером на карте. */
  def circle    : CircleGs

  /** Период размещения. */
  def period    : MDatesPeriod


  /** Приведение периода размещения в понятиях формы к периоду размещения в датах. */
  def interval = new Interval(period.dateStart.toDateTimeAtStartOfDay, period.dateEnd.toDateTimeAtStartOfDay)

}


trait IAdvFormResult extends IAdvGeoTagsInfo {

  /** Состояние карты, используется при unbind(). */
  def mapState  : MapViewState

}


case class MAdvFormResult(
  tags      : List[MTagBinded],
  mapState  : MapViewState,
  circle    : CircleGs,
  period    : MDatesPeriod
)
  extends IAdvFormResult
