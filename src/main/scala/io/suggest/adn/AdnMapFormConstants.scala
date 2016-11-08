package io.suggest.adn

import io.suggest.adv.AdvConstants
import io.suggest.common.maps.MapFormConstants

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.11.16 11:42
  * Description: Константы для формы/страницы размещения ADN-узла на карте.
  */
object AdnMapFormConstants {

  def ID_PREFIX = "amf_"

  /** id HTML-формы. */
  def FORM_ID = ID_PREFIX + "f"


  /** Поля формы. */
  object Fields {

    def POINT_FN  = MapFormConstants.CENTER_FN

    def STATE_FN  = MapFormConstants.STATE_FN

    def PERIOD_FN = AdvConstants.PERIOD_FN

  }


  /** Инпуты для точки размещения. */
  object Point {

    def INPUT_ID_LON = ID_PREFIX + MapFormConstants.INPUT_ID_LON
    def INPUT_ID_LAT = ID_PREFIX + MapFormConstants.INPUT_ID_LAT

  }

}
