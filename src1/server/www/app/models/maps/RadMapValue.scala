package models.maps

import io.suggest.geo.CircleGs

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.03.16 16:49
  * Description: Модель данных от rad_map после bind'а.
  */
case class RadMapValue(
  state   : MapViewState,
  circle  : CircleGs
)
