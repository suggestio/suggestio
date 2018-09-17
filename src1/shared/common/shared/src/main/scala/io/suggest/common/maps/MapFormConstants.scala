package io.suggest.common.maps

import io.suggest.common.html.HtmlConstants.ATTR_PREFIX

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.11.16 11:50
  * Description: Константы для форм с картой.
  */
object MapFormConstants {

  /** Префикс разных id'шников, объявленных здесь. */
  def ID_PREFIX = "mf_"

  def ATTR_IMG_WIDTH        = ATTR_PREFIX + "w"
  def ATTR_IMG_HEIGHT       = ATTR_PREFIX + "h"
  def ATTR_IMG_ANCHOR_X     = ATTR_PREFIX + "x"
  def ATTR_IMG_ANCHOR_Y     = ATTR_PREFIX + "y"


  /** id img-тега, хранящего адресок картинки с геомаркером карты. */
  def IMG_ID_MARKER        = ID_PREFIX + "ic"
  /** id img-тега, хранящего вариант retina-картинки геомаркера. */
  def IMG_ID_MARKER_RETINA = ID_PREFIX + "icr"
  /** id img-тега с тенью маркера. */
  def IMG_ID_MARKER_SHADOW = ID_PREFIX + "ics"

}

