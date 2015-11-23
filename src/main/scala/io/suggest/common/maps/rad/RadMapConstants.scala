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

  def INPUT_ID_LAT  = _ID_PREFIX + "ma"
  def INPUT_ID_LON  = _ID_PREFIX + "mo"
  def INPUT_ID_ZOOM = _ID_PREFIX + "mz"

  def INPUT_ID_CIRCLE_LAT     = _ID_PREFIX + "ca"
  def INPUT_ID_CIRCLE_LON     = _ID_PREFIX + "co"
  def INPUT_ID_CIRCLE_RADIUS  = _ID_PREFIX + "cr"


  /** id img-тега, хранящего адресок картинки с геомаркером карты. */
  def IMG_ID_MARKER        = _ID_PREFIX + "im"
  /** id img-тега, хранящего вариант retina-картинки геомаркера. */
  def IMG_ID_MARKER_RETINA = _ID_PREFIX + "imr"
  /** id img-тега с тенью маркера. */
  def IMG_ID_MARKER_SHADOW = _ID_PREFIX + "is"

  def ATTR_PREFIX = "data-"
  def ATTR_IMG_WIDTH        = ATTR_PREFIX + "w"
  def ATTR_IMG_HEIGHT       = ATTR_PREFIX + "h"
  def ATTR_IMG_ANCHOR_X     = ATTR_PREFIX + "x"
  def ATTR_IMG_ANCHOR_Y     = ATTR_PREFIX + "y"

}
