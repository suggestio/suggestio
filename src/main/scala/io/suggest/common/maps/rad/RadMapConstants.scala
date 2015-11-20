package io.suggest.common.maps.rad

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.11.15 17:53
  * Description: cross-compiled константы для rad map.
  */
object RadMapConstants {

  def _ID_PREFIX = "rm_"

  /** id элемента-контейнера карты. */
  def CONTAINER_ID  = _ID_PREFIX + "cont"

  def INPUT_ID_LAT  = _ID_PREFIX + "a"
  def INPUT_ID_LON  = _ID_PREFIX + "o"
  def INPUT_ID_ZOOM = _ID_PREFIX + "z"

}
