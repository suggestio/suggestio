package models.madn.mapf

import io.suggest.model.geo.GeoPoint
import models.adv.form.{IAdvFormResult, MDatesPeriod}
import models.maps.MapViewState

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.11.16 11:36
  * Description: Модель результата биндинга формы размещения ADN-узла на карте мира.
  * @param point Точка, в которой размещаемся.
  * @param mapState Состояние карты: центр, зум и т.д.
  * @param period Период размещения узла на карте.
  */

case class MAdnMapFormRes(
  point                     : GeoPoint,
  mapState                  : MapViewState,
  override val period       : MDatesPeriod
)
  extends IAdvFormResult
