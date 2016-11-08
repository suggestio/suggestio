package models.maps

import io.suggest.model.geo.GeoPoint

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.11.15 22:17
  * Description: Модель состояния отображения карты. Т.е. то, что смотрит пользователь.
  */

case class MapViewState(
  center  : GeoPoint,
  zoom    : Int = 10
)
