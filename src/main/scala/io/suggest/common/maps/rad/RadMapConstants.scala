package io.suggest.common.maps.rad

import io.suggest.common.maps.MapFormConstants

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.11.15 17:53
  * Description: cross-compiled константы для rad map.
  */
object RadMapConstants {

  def _ID_PREFIX = "r" + MapFormConstants.ID_PREFIX

  def INPUT_ID_CIRCLE_LAT     = _ID_PREFIX + "ca"
  def INPUT_ID_CIRCLE_LON     = _ID_PREFIX + "co"
  def INPUT_ID_CIRCLE_RADIUS  = _ID_PREFIX + "cr"


  /** id img-тега с маркером радиуса охвата. */
  def IMG_ID_RADIUS_MARKER = _ID_PREFIX + "ir"


  // Константы HTML-форм, содержащий rad-map.

  def CIRCLE_FN = "circle"
  def RADIUS_FN = "radius"

}
