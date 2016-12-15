package models.maps

import io.suggest.geo.MGeoPoint

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.11.15 22:17
  * Description: Модель состояния отображения карты. Т.е. то, что смотрит пользователь.
  */

case class MapViewState(
  center  : MGeoPoint,
  zoom    : Int = 10
)
