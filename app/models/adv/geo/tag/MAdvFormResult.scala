package models.adv.geo.tag

import io.suggest.model.geo.CircleGs
import models.adv.form.MDatesPeriod
import models.maps.MapViewState
import models.mtag.MTagBinded

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
  def dates     : MDatesPeriod

}


trait IAdvFormResult extends IAdvGeoTagsInfo {

  /** Состояние карты, используется при unbind(). */
  def mapState  : MapViewState

}


case class MAdvFormResult(
  override val tags       : List[MTagBinded],
  override val mapState   : MapViewState,
  override val circle     : CircleGs,
  override val dates      : MDatesPeriod
)
  extends IAdvFormResult
