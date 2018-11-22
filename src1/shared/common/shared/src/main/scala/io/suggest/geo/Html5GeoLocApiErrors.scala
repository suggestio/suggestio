package io.suggest.geo

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.11.18 13:10
  * Description: Коды ошибок геолокации в HTML5 Geolocation API.
  * @see [[https://developer.mozilla.org/en-US/docs/Web/API/PositionError]]
  */
object Html5GeoLocApiErrors {

  val PERMISSION_DENIED = 1

  val POSITION_UNAVAILABLE = 2

  val TIMEOUT = 3

}
